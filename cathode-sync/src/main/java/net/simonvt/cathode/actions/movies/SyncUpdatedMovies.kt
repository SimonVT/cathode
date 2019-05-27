/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
package net.simonvt.cathode.actions.movies

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.text.format.DateUtils
import androidx.work.WorkManager
import net.simonvt.cathode.actions.PagedAction
import net.simonvt.cathode.actions.PagedResponse
import net.simonvt.cathode.api.entity.UpdatedItem
import net.simonvt.cathode.api.service.MoviesService
import net.simonvt.cathode.api.util.TimeUtils
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.settings.Timestamps
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import retrofit2.Call
import javax.inject.Inject

class SyncUpdatedMovies @Inject constructor(
  private val context: Context,
  private val workManager: WorkManager,
  private val moviesService: MoviesService,
  private val movieHelper: MovieDatabaseHelper
) : PagedAction<Unit, UpdatedItem>() {

  private val currentTime = System.currentTimeMillis()

  override fun key(params: Unit): String = "SyncUpdatedMovies"

  override fun getCall(params: Unit, page: Int): Call<List<UpdatedItem>> {
    val lastUpdated =
      Timestamps.get(context).getLong(Timestamps.MOVIES_LAST_UPDATED, currentTime)
    val millis = lastUpdated - 12 * DateUtils.HOUR_IN_MILLIS
    val updatedSince = TimeUtils.getIsoTime(millis)
    return moviesService.updated(updatedSince, page, LIMIT)
  }

  override suspend fun handleResponse(
    params: Unit,
    pagedResponse: PagedResponse<Unit, UpdatedItem>
  ) {
    val ops = arrayListOf<ContentProviderOperation>()

    var page: PagedResponse<Unit, UpdatedItem>? = pagedResponse
    do {
      for (item in page!!.response) {
        val updatedAt = item.updated_at.timeInMillis
        val movie = item.movie!!
        val traktId = movie.ids.trakt!!

        val movieId = movieHelper.getId(traktId)
        if (movieId != -1L) {
          if (movieHelper.isUpdated(traktId, updatedAt)) {
            val values = ContentValues()
            values.put(MovieColumns.NEEDS_SYNC, true)
            ops.add(
              ContentProviderOperation.newUpdate(Movies.withId(movieId))
                .withValues(values)
                .build()
            )
          }
        }
      }

      page = page.nextPage()
    } while (page != null)

    context.contentResolver.batch(ops)
  }

  override fun onDone() {
    workManager.enqueueUniqueNow(SyncPendingMoviesWorker.TAG, SyncPendingMoviesWorker::class.java)
    Timestamps.get(context)
      .edit()
      .putLong(Timestamps.MOVIES_LAST_UPDATED, currentTime)
      .apply()
  }

  companion object {
    private const val LIMIT = 100
  }
}
