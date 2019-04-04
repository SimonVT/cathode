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
import net.simonvt.cathode.actions.user.SyncMoviesWatchlist.Params
import net.simonvt.cathode.api.entity.WatchlistItem
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import retrofit2.Call
import javax.inject.Inject

class SyncMoviesWatchlist @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val syncService: SyncService,
  private val workManager: WorkManager
) : CallAction<Params, List<WatchlistItem>>() {

  override fun key(params: Params): String = "SyncMoviesWatchlist"

  override fun getCall(params: Params): Call<List<WatchlistItem>> = syncService.getMovieWatchlist()

  override suspend fun handleResponse(params: Params, response: List<WatchlistItem>) {
    val movieIds = mutableListOf<Long>()
    val localWatchlist = context.contentResolver.query(
      Movies.MOVIES,
      arrayOf(MovieColumns.ID),
      MovieColumns.IN_WATCHLIST
    )
    localWatchlist.forEach { cursor -> movieIds.add(cursor.getLong(MovieColumns.ID)) }
    localWatchlist.close()

    for (watchlistItem in response) {
      val listedAt = watchlistItem.listed_at.timeInMillis
      val traktId = watchlistItem.movie!!.ids.trakt!!

      val result = movieHelper.getIdOrCreate(traktId)
      val movieId = result.movieId

      if (!movieIds.remove(movieId)) {
        movieHelper.setIsInWatchlist(movieId, true, listedAt)
      }
    }

    for (movieId in movieIds) {
      movieHelper.setIsInWatchlist(movieId, false)
    }

    workManager.enqueueUniqueNow(SyncPendingMoviesWorker.TAG, SyncPendingMoviesWorker::class.java)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.MOVIE_WATCHLIST, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)
}
