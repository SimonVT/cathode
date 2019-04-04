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
import net.simonvt.cathode.actions.user.SyncMoviesCollection.Params
import net.simonvt.cathode.api.entity.CollectionItem
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

class SyncMoviesCollection @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val syncService: SyncService,
  private val workManager: WorkManager
) : CallAction<Params, List<CollectionItem>>() {

  override fun key(params: Params): String = "SyncMoviesCollection"

  override fun getCall(params: Params): Call<List<CollectionItem>> =
    syncService.getMovieCollection()

  override suspend fun handleResponse(params: Params, response: List<CollectionItem>) {
    val movieIds = mutableListOf<Long>()
    val localCollection = context.contentResolver.query(
      Movies.MOVIES,
      arrayOf(MovieColumns.ID),
      MovieColumns.IN_COLLECTION
    )
    localCollection.forEach { cursor -> movieIds.add(cursor.getLong(MovieColumns.ID)) }
    localCollection.close()

    for (collectionItem in response) {
      val traktId = collectionItem.movie!!.ids.trakt!!
      val result = movieHelper.getIdOrCreate(traktId)
      val movieId = result.movieId
      val collectedAt = collectionItem.collected_at!!.timeInMillis

      if (!movieIds.remove(movieId)) {
        movieHelper.setIsInCollection(movieId, true, collectedAt)
      }
    }

    for (movieId in movieIds) {
      movieHelper.setIsInCollection(movieId, false)
    }

    workManager.enqueueUniqueNow(SyncPendingMoviesWorker.TAG, SyncPendingMoviesWorker::class.java)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.MOVIE_COLLECTION, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)
}
