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

package net.simonvt.cathode.ui.movies.watchlist

import android.content.Context
import androidx.lifecycle.LiveData
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncMoviesWatchlist
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Movie
import net.simonvt.cathode.entitymapper.MovieListMapper
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.movies.MoviesAdapter
import javax.inject.Inject

class MovieWatchlistViewModel @Inject constructor(
  context: Context,
  private val syncMoviesWatchlist: SyncMoviesWatchlist
) : RefreshableViewModel() {

  val movies: LiveData<List<Movie>>

  init {
    movies = MappedCursorLiveData(
      context,
      Movies.MOVIES_WATCHLIST,
      MoviesAdapter.PROJECTION,
      null,
      null,
      Movies.DEFAULT_SORT,
      MovieListMapper()
    )
  }

  override suspend fun onRefresh() {
    syncMoviesWatchlist.invokeSync(SyncMoviesWatchlist.Params())
  }
}
