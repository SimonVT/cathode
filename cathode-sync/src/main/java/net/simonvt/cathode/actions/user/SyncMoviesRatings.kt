/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.user.SyncMoviesRatings.Params
import net.simonvt.cathode.api.entity.RatingItem
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncMoviesRatings @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val syncService: SyncService
) : CallAction<Params, List<RatingItem>>() {

  override fun key(params: Params): String = "SyncMoviesRatings"

  override fun getCall(params: Params): Call<List<RatingItem>> = syncService.getMovieRatings()

  override suspend fun handleResponse(params: Params, response: List<RatingItem>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val movieIds = mutableListOf<Long>()

    val movies = context.contentResolver.query(
      Movies.MOVIES,
      arrayOf(MovieColumns.ID),
      MovieColumns.RATED_AT + ">0"
    )
    movies.forEach { cursor -> movieIds.add(cursor.getLong(MovieColumns.ID)) }
    movies.close()

    for (rating in response) {
      val traktId = rating.movie!!.ids.trakt!!
      val result = movieHelper.getIdOrCreate(traktId)
      val movieId = result.movieId
      movieIds.remove(movieId)

      val op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
        .withValue(MovieColumns.USER_RATING, rating.rating)
        .withValue(MovieColumns.RATED_AT, rating.rated_at.timeInMillis)
        .build()
      ops.add(op)
    }

    for (movieId in movieIds) {
      val op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
        .withValue(MovieColumns.USER_RATING, 0)
        .withValue(MovieColumns.RATED_AT, 0)
        .build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.MOVIE_RATING, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)
}
