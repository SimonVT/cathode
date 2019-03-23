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

import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.movies.SyncMovie.Params
import net.simonvt.cathode.api.entity.Movie
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.MoviesService
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import retrofit2.Call
import javax.inject.Inject

class SyncMovie @Inject constructor(
  private val moviesService: MoviesService,
  private val movieHelper: MovieDatabaseHelper
) : CallAction<Params, Movie>() {

  override fun getCall(params: Params): Call<Movie> =
    moviesService.getSummary(params.traktId, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: Movie) {
    movieHelper.fullUpdate(response)
  }

  data class Params(val traktId: Long)

  companion object {

    fun key(traktId: Long): String {
      return "SyncMovie&traktId=$traktId"
    }
  }
}
