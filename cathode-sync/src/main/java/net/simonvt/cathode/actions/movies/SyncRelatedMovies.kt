/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.movies.SyncRelatedMovies.Params
import net.simonvt.cathode.api.entity.Movie
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.MoviesService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseContract.RelatedMoviesColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import javax.inject.Inject

class SyncRelatedMovies @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val moviesService: MoviesService
) : CallAction<Params, List<Movie>>() {

  override fun getCall(params: Params): Call<List<Movie>> =
    moviesService.getRelated(params.traktId, RELATED_COUNT, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: List<Movie>) {
    val movieId = movieHelper.getId(params.traktId)

    val ops = arrayListOf<ContentProviderOperation>()
    val relatedIds = mutableListOf<Long>()

    val related = context.contentResolver.query(
      RelatedMovies.fromMovie(movieId),
      arrayOf(Tables.MOVIE_RELATED + "." + RelatedMoviesColumns.ID)
    )
    related.forEach { cursor -> relatedIds.add(cursor.getLong(RelatedMoviesColumns.ID)) }

    related.close()

    for ((index, movie) in response.withIndex()) {
      val relatedMovieId = movieHelper.partialUpdate(movie)

      val op = ContentProviderOperation.newInsert(RelatedMovies.RELATED)
        .withValue(RelatedMoviesColumns.MOVIE_ID, movieId)
        .withValue(RelatedMoviesColumns.RELATED_MOVIE_ID, relatedMovieId)
        .withValue(RelatedMoviesColumns.RELATED_INDEX, index)
        .build()
      ops.add(op)
    }

    for (id in relatedIds) {
      ops.add(ContentProviderOperation.newDelete(RelatedMovies.withId(id)).build())
    }

    ops.add(
      ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValue(
        MovieColumns.LAST_RELATED_SYNC,
        System.currentTimeMillis()
      ).build()
    )

    context.contentResolver.batch(ops)
  }

  data class Params(val traktId: Long)

  companion object {

    private const val RELATED_COUNT = 50

    fun key(traktId: Long): String {
      return "SyncRelatedMovies&traktId=$traktId"
    }
  }
}
