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

import android.content.Context
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.services.MoviesService
import net.simonvt.cathode.actions.TmdbCallAction
import net.simonvt.cathode.actions.movies.SyncMovieImages.Params
import net.simonvt.cathode.images.MovieRequestHandler
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import retrofit2.Call
import javax.inject.Inject

class SyncMovieImages @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val moviesService: MoviesService
) : TmdbCallAction<Params, Movie>() {

  override fun getCall(params: Params): Call<Movie> = moviesService.summary(params.tmdbId, "en")

  override suspend fun handleResponse(params: Params, response: Movie) {
    val movieId = movieHelper.getIdFromTmdb(params.tmdbId)
    MovieRequestHandler.retainImages(context, movieId, response)
  }

  data class Params(val tmdbId: Int)

  companion object {

    fun key(tmdbId: Int): String {
      return "SyncMovieImages&traktId=$tmdbId"
    }
  }
}
