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

package net.simonvt.cathode.ui.suggestions.movies

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.movies.SyncTrendingMovies
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Movie
import net.simonvt.cathode.entitymapper.MovieListMapper
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.SuggestionsTimestamps
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.movies.MoviesAdapter
import net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesFragment.SortBy
import javax.inject.Inject

class TrendingMoviesViewModel @Inject constructor(
  private val context: Context,
  private val syncTrendingMovies: SyncTrendingMovies
) : RefreshableViewModel() {

  val trending: MappedCursorLiveData<List<Movie>>

  private var sortBy: SortBy

  init {
    sortBy = SortBy.fromValue(
      Settings.get(context).getString(
        Settings.Sort.MOVIE_TRENDING,
        SortBy.VIEWERS.key
      )!!
    )
    trending = MappedCursorLiveData(
      context,
      Movies.TRENDING,
      MoviesAdapter.PROJECTION,
      null,
      null,
      sortBy.sortOrder,
      MovieListMapper()
    )

    viewModelScope.launch {
      if (System.currentTimeMillis() > SuggestionsTimestamps.get(context).getLong(
          SuggestionsTimestamps.MOVIES_TRENDING,
          0L
        ) + SYNC_INTERNAL
      ) {
        ActionManager.invokeAsync(SyncTrendingMovies.key(), syncTrendingMovies, Unit)
      }
    }
  }

  fun setSortBy(sortBy: SortBy) {
    this.sortBy = sortBy
    trending.setSortOrder(sortBy.sortOrder)
  }

  override suspend fun onRefresh() {
    ActionManager.invokeSync(SyncTrendingMovies.key(), syncTrendingMovies, Unit)
  }

  companion object {

    const val SYNC_INTERNAL = 6 * DateUtils.HOUR_IN_MILLIS
  }
}
