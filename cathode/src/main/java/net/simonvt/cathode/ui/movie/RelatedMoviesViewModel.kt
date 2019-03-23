/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.movie

import android.content.Context
import androidx.lifecycle.LiveData
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.movies.SyncRelatedMovies
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Movie
import net.simonvt.cathode.entitymapper.MovieListMapper
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.movies.MoviesAdapter
import javax.inject.Inject

class RelatedMoviesViewModel @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val syncRelatedMovies: SyncRelatedMovies
) : RefreshableViewModel() {

  private var movieId = -1L

  lateinit var movies: LiveData<List<Movie>>
    private set

  fun setMovieId(movieId: Long) {
    if (this.movieId == -1L) {
      this.movieId = movieId
      movies = MappedCursorLiveData(
        context,
        RelatedMovies.fromMovie(movieId),
        MoviesAdapter.PROJECTION,
        null,
        null,
        null,
        MovieListMapper()
      )
    }
  }

  override suspend fun onRefresh() {
    val traktId = movieHelper.getTraktId(movieId)
    ActionManager.invokeSync(
      SyncRelatedMovies.key(traktId),
      syncRelatedMovies,
      SyncRelatedMovies.Params(traktId)
    )
  }
}
