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
import net.simonvt.cathode.api.entity.WatchedItem
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.work.WorkManagerUtils
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import retrofit2.Call
import javax.inject.Inject

class SyncWatchedMovies @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val syncService: SyncService,
  private val workManager: WorkManager
) : CallAction<Unit, List<WatchedItem>>() {

  override fun getCall(params: Unit): Call<List<WatchedItem>> = syncService.getWatchedMovies()

  override suspend fun handleResponse(params: Unit, response: List<WatchedItem>) {
    val movieIds = mutableListOf<Long>()
    val localWatched = context.contentResolver.query(
      Movies.MOVIES,
      arrayOf(MovieColumns.ID),
      MovieColumns.WATCHED
    )
    localWatched.forEach { cursor -> movieIds.add(cursor.getLong(MovieColumns.ID)) }
    localWatched.close()

    for (watchedItem in response) {
      val traktId = watchedItem.movie!!.ids.trakt!!
      val result = movieHelper.getIdOrCreate(traktId)
      val movieId = result.movieId
      val watchedAt = watchedItem.last_watched_at.timeInMillis

      if (!movieIds.remove(movieId)) {
        movieHelper.addToHistory(movieId, watchedAt)
      }
    }

    for (movieId in movieIds) {
      movieHelper.removeFromHistory(movieId)
    }

    WorkManagerUtils.enqueueUniqueNow(
      workManager,
      SyncPendingMoviesWorker.TAG,
      SyncPendingMoviesWorker::class.java
    )
  }

  companion object {

    fun key() = "SyncWatchedMovies"
  }
}
