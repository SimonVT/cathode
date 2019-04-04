/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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

import android.content.Context
import androidx.work.WorkManager
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.user.SyncShowsWatchlist.Params
import net.simonvt.cathode.api.entity.WatchlistItem
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import retrofit2.Call
import javax.inject.Inject

class SyncShowsWatchlist @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val syncService: SyncService,
  private val workManager: WorkManager
) : CallAction<Params, List<WatchlistItem>>() {

  override fun key(params: Params): String = "SyncShowsWatchlist"

  override fun getCall(params: Params): Call<List<WatchlistItem>> = syncService.getShowWatchlist()

  override suspend fun handleResponse(params: Params, response: List<WatchlistItem>) {
    val showIds = mutableListOf<Long>()

    val localWatchlist = context.contentResolver.query(
      Shows.SHOWS,
      arrayOf(DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ID),
      ShowColumns.IN_WATCHLIST
    )
    localWatchlist.forEach { cursor -> showIds.add(cursor.getLong(ShowColumns.ID)) }
    localWatchlist.close()

    for (watchlistItem in response) {
      val listedAt = watchlistItem.listed_at.timeInMillis
      val traktId = watchlistItem.show!!.ids.trakt!!
      val showResult = showHelper.getIdOrCreate(traktId)
      val showId = showResult.showId

      if (!showIds.remove(showId)) {
        showHelper.setIsInWatchlist(showId, true, listedAt)
      }
    }

    for (showId in showIds) {
      showHelper.setIsInWatchlist(showId, false)
    }

    workManager.enqueueUniqueNow(SyncPendingShowsWorker.TAG, SyncPendingShowsWorker::class.java)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.SHOW_WATCHLIST, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)
}
