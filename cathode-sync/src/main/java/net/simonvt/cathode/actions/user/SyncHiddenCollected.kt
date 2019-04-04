/*
 * Copyright (C) 2017 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.actions.user

import android.content.ContentProviderOperation
import android.content.Context
import androidx.work.WorkManager
import net.simonvt.cathode.actions.PagedAction
import net.simonvt.cathode.api.entity.HiddenItem
import net.simonvt.cathode.api.enumeration.HiddenSection
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import retrofit2.Call
import javax.inject.Inject

class SyncHiddenCollected @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val usersService: UsersService,
  private val workManager: WorkManager
) : PagedAction<Unit, HiddenItem>() {

  override fun key(params: Unit): String = "SyncHiddenCollected"

  override fun getCall(params: Unit, page: Int): Call<List<HiddenItem>> =
    usersService.getHiddenItems(HiddenSection.PROGRESS_COLLECTED, null, page, 25)

  override suspend fun handleResponse(params: Unit, page: Int, response: List<HiddenItem>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val unhandledShows = mutableListOf<Long>()
    val unhandledSeasons = mutableListOf<Long>()

    val hiddenShows = context.contentResolver.query(
      Shows.SHOWS,
      arrayOf(ShowColumns.ID),
      ShowColumns.HIDDEN_COLLECTED + "=1"
    )
    hiddenShows.forEach { cursor -> unhandledShows.add(cursor.getLong(ShowColumns.ID)) }
    hiddenShows.close()

    val hiddenSeasons = context.contentResolver.query(
      Seasons.SEASONS,
      arrayOf(SeasonColumns.ID),
      SeasonColumns.HIDDEN_COLLECTED + "=1"
    )
    hiddenSeasons.forEach { cursor -> unhandledSeasons.add(cursor.getLong(SeasonColumns.ID)) }
    hiddenSeasons.close()

    for (hiddenItem in response) {
      when (hiddenItem.type) {
        ItemType.SHOW -> {
          val show = hiddenItem.show!!
          val traktId = show.ids.trakt!!
          val showResult = showHelper.getIdOrCreate(traktId)
          val showId = showResult.showId

          if (!unhandledShows.remove(showId)) {
            val op = ContentProviderOperation.newUpdate(Shows.withId(showId))
              .withValue(ShowColumns.HIDDEN_COLLECTED, 1)
              .build()
            ops.add(op)
          }
        }

        ItemType.SEASON -> {
          val show = hiddenItem.show!!
          val season = hiddenItem.season!!

          val traktId = show.ids.trakt!!
          val showResult = showHelper.getIdOrCreate(traktId)
          val showId = showResult.showId

          val seasonNumber = season.number
          val result = seasonHelper.getIdOrCreate(showId, seasonNumber)
          val seasonId = result.id
          if (result.didCreate) {
            if (!showResult.didCreate) {
              showHelper.markPending(showId)
            }
          }

          if (!unhandledSeasons.remove(seasonId)) {
            val op = ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
              .withValue(SeasonColumns.HIDDEN_COLLECTED, 1)
              .build()
            ops.add(op)
          }
        }

        else -> throw RuntimeException("Unknown item type: ${hiddenItem.type}")
      }
    }

    workManager.enqueueUniqueNow(SyncPendingShowsWorker.TAG, SyncPendingShowsWorker::class.java)
    workManager.enqueueUniqueNow(SyncPendingMoviesWorker.TAG, SyncPendingMoviesWorker::class.java)

    for (showId in unhandledShows) {
      val op = ContentProviderOperation.newUpdate(Shows.withId(showId))
        .withValue(ShowColumns.HIDDEN_COLLECTED, 0)
        .build()
      ops.add(op)
    }

    for (seasonId in unhandledSeasons) {
      val op = ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
        .withValue(SeasonColumns.HIDDEN_COLLECTED, 0)
        .build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)
  }
}
