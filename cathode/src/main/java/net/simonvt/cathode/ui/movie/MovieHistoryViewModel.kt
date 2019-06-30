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
import androidx.lifecycle.ViewModel
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.entitymapper.MovieMapper
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import javax.inject.Inject

class MovieHistoryViewModel @Inject constructor(
  private val context: Context,
  private val syncService: SyncService,
  private val movieHelper: MovieDatabaseHelper
) : ViewModel() {

  private var movieId = -1L

  var movie: LiveData<Movie>? = null
    private set
  var history: MovieHistoryLiveData? = null
    private set

  fun setMovieId(movieId: Long) {
    if (this.movieId == -1L) {
      this.movieId = movieId

      movie = MappedCursorLiveData(
        context,
        Movies.withId(movieId),
        MovieMapper.projection,
        mapper = MovieMapper
      )
      history = MovieHistoryLiveData(movieId, syncService, movieHelper)
    }
  }
}
