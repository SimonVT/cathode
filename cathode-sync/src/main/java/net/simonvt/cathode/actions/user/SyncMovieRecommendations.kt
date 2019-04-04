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

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.api.entity.Movie
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.RecommendationsService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.SuggestionsTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncMovieRecommendations @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val recommendationsService: RecommendationsService
) : CallAction<Unit, List<Movie>>() {

  override fun key(params: Unit): String = "SyncMovieRecommendations"

  override fun getCall(params: Unit): Call<List<Movie>> =
    recommendationsService.movies(LIMIT, Extended.FULL)

  override suspend fun handleResponse(params: Unit, response: List<Movie>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val movieIds = mutableListOf<Long>()

    val localMovies = context.contentResolver.query(Movies.RECOMMENDED)
    localMovies.forEach { cursor -> movieIds.add(cursor.getLong(MovieColumns.ID)) }
    localMovies.close()

    response.forEachIndexed { index, movie ->
      val movieId = movieHelper.partialUpdate(movie)
      movieIds.remove(movieId)

      val values = ContentValues()
      values.put(MovieColumns.RECOMMENDATION_INDEX, index)
      val op = ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(values).build()
      ops.add(op)
    }

    for (id in movieIds) {
      val op = ContentProviderOperation.newUpdate(Movies.withId(id))
        .withValue(MovieColumns.RECOMMENDATION_INDEX, -1)
        .build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)

    SuggestionsTimestamps.get(context)
      .edit()
      .putLong(SuggestionsTimestamps.MOVIES_RECOMMENDED, System.currentTimeMillis())
      .apply()
  }

  companion object {
    private const val LIMIT = 50
  }
}
