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

package net.simonvt.cathode.ui.movies.collected

import android.content.Context
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncMoviesCollection
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.entitymapper.MovieListMapper
import net.simonvt.cathode.entitymapper.MovieMapper
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.movies.collected.CollectedMoviesFragment.SortBy
import javax.inject.Inject

class CollectedMoviesViewModel @Inject constructor(
  private val context: Context,
  private val syncMoviesCollection: SyncMoviesCollection
) : RefreshableViewModel() {

  val movies: MappedCursorLiveData<List<Movie>>

  private var sortBy: SortBy

  init {
    sortBy = SortBy.fromValue(
      Settings.get(context).getString(
        Settings.Sort.MOVIE_COLLECTED,
        SortBy.TITLE.key
      )!!
    )
    movies = MappedCursorLiveData(
      context,
      Movies.MOVIES_COLLECTED,
      MovieMapper.projection,
      null,
      null,
      sortBy.sortOrder,
      MovieListMapper
    )
  }

  fun setSortBy(sortBy: SortBy) {
    this.sortBy = sortBy
    movies.setSortOrder(sortBy.sortOrder)
  }

  override suspend fun onRefresh() {
    syncMoviesCollection.invokeSync(SyncMoviesCollection.Params())
  }
}
