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
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.movies.SyncAnticipatedMovies
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.entitymapper.MovieListMapper
import net.simonvt.cathode.entitymapper.MovieMapper
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.SuggestionsTimestamps
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.suggestions.movies.AnticipatedMoviesFragment.SortBy
import javax.inject.Inject

class AnticipatedMoviesViewModel @Inject constructor(
  private val context: Context,
  private val syncAnticipatedMovies: SyncAnticipatedMovies
) : RefreshableViewModel() {

  val anticipated: MappedCursorLiveData<List<Movie>>

  private var sortBy: SortBy? = null

  init {
    sortBy = SortBy.fromValue(
      Settings.get(context).getString(
        Settings.Sort.MOVIE_ANTICIPATED,
        SortBy.ANTICIPATED.key
      )!!
    )
    anticipated = MappedCursorLiveData(
      context,
      Movies.ANTICIPATED,
      MovieMapper.projection,
      null,
      null,
      sortBy!!.sortOrder,
      MovieListMapper
    )

    viewModelScope.launch {
      if (System.currentTimeMillis() > SuggestionsTimestamps.get(context).getLong(
          SuggestionsTimestamps.MOVIES_ANTICIPATED,
          0L
        ) + SYNC_INTERNAL
      ) {
        syncAnticipatedMovies.invokeAsync(Unit)
      }
    }
  }

  fun setSortBy(sortBy: SortBy) {
    this.sortBy = sortBy
    anticipated.setSortOrder(sortBy.sortOrder)
  }

  override suspend fun onRefresh() {
    syncAnticipatedMovies.invokeSync(Unit)
  }

  companion object {

    private const val SYNC_INTERNAL = DateUtils.DAY_IN_MILLIS
  }
}
