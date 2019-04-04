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
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import retrofit2.Call
import javax.inject.Inject

class SyncHiddenRecommendations @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val movieHelper: MovieDatabaseHelper,
  private val usersService: UsersService,
  private val workManager: WorkManager
) : PagedAction<Unit, HiddenItem>() {

  override fun key(params: Unit): String = "SyncHiddenRecommendations"

  override fun getCall(params: Unit, page: Int): Call<List<HiddenItem>> =
    usersService.getHiddenItems(HiddenSection.RECOMMENDATIONS, null, page, 25)

  override suspend fun handleResponse(params: Unit, page: Int, response: List<HiddenItem>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val unhandledShows = mutableListOf<Long>()
    val unhandledMovies = mutableListOf<Long>()

    val hiddenShows = context.contentResolver.query(
      Shows.SHOWS,
      arrayOf(ShowColumns.ID),
      ShowColumns.HIDDEN_RECOMMENDATIONS + "=1"
    )
    hiddenShows.forEach { cursor -> unhandledShows.add(cursor.getLong(ShowColumns.ID)) }
    hiddenShows.close()

    val hiddenMovies = context.contentResolver.query(
      Movies.MOVIES,
      arrayOf(MovieColumns.ID),
      MovieColumns.HIDDEN_RECOMMENDATIONS + "=1"
    )
    hiddenMovies.forEach { cursor -> unhandledMovies.add(cursor.getLong(MovieColumns.ID)) }
    hiddenMovies.close()

    for (hiddenItem in response) {
      when (hiddenItem.type) {
        ItemType.SHOW -> {
          val show = hiddenItem.show!!
          val traktId = show.ids.trakt!!
          val showResult = showHelper.getIdOrCreate(traktId)
          val showId = showResult.showId

          if (!unhandledShows.remove(showId)) {
            val op = ContentProviderOperation.newUpdate(Shows.withId(showId))
              .withValue(ShowColumns.HIDDEN_RECOMMENDATIONS, 1)
              .build()
            ops.add(op)
          }
        }

        ItemType.MOVIE -> {
          val movie = hiddenItem.movie!!
          val traktId = movie.ids.trakt!!
          val result = movieHelper.getIdOrCreate(traktId)
          val movieId = result.movieId

          if (!unhandledMovies.remove(movieId)) {
            val op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
              .withValue(MovieColumns.HIDDEN_RECOMMENDATIONS, 1)
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
        .withValue(ShowColumns.HIDDEN_RECOMMENDATIONS, 0)
        .build()
      ops.add(op)
    }

    for (movieId in unhandledMovies) {
      val op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
        .withValue(MovieColumns.HIDDEN_RECOMMENDATIONS, 0)
        .build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)
  }
}
