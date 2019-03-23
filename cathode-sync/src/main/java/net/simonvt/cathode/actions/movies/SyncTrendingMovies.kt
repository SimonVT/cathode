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
package net.simonvt.cathode.actions.movies

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.api.entity.TrendingItem
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.MoviesService
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

class SyncTrendingMovies @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val moviesService: MoviesService
) : CallAction<Unit, List<TrendingItem>>() {

  override fun getCall(params: Unit): Call<List<TrendingItem>> =
    moviesService.getTrendingMovies(LIMIT, Extended.FULL)

  override suspend fun handleResponse(params: Unit, response: List<TrendingItem>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val trendingIds = mutableListOf<Long>()

    val localMovies = context.contentResolver.query(Movies.TRENDING, arrayOf(MovieColumns.ID))
    localMovies.forEach { cursor -> trendingIds.add(cursor.getLong(MovieColumns.ID)) }
    localMovies.close()

    response.forEachIndexed { index, trendingItem ->
      val movie = trendingItem.movie!!
      val movieId = movieHelper.partialUpdate(movie)
      trendingIds.remove(movieId)

      val values = ContentValues()
      values.put(MovieColumns.TRENDING_INDEX, index)
      val op = ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(values).build()
      ops.add(op)
    }

    for (movieId in trendingIds) {
      val values = ContentValues()
      values.put(MovieColumns.TRENDING_INDEX, -1)
      val op = ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(values).build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)

    SuggestionsTimestamps.get(context)
      .edit()
      .putLong(SuggestionsTimestamps.MOVIES_TRENDING, System.currentTimeMillis())
      .apply()
  }

  companion object {

    private const val LIMIT = 50

    fun key() = "SyncTrendingMovies"
  }
}
