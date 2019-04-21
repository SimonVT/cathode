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

package net.simonvt.cathode.ui.dashboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.movies.SyncTrendingMovies
import net.simonvt.cathode.actions.shows.SyncTrendingShows
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.entitymapper.EpisodeListMapper
import net.simonvt.cathode.entitymapper.EpisodeMapper
import net.simonvt.cathode.entitymapper.MovieListMapper
import net.simonvt.cathode.entitymapper.MovieMapper
import net.simonvt.cathode.entitymapper.ShowListMapper
import net.simonvt.cathode.entitymapper.ShowMapper
import net.simonvt.cathode.entitymapper.ShowWithEpisodeListMapper
import net.simonvt.cathode.entitymapper.ShowWithEpisodeMapper
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.settings.SuggestionsTimestamps
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference.UpcomingSortByListener
import net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesViewModel
import net.simonvt.cathode.ui.suggestions.shows.TrendingShowsViewModel
import javax.inject.Inject

class DashboardViewModel @Inject constructor(
  private val context: Context,
  private val upcomingSortByPreference: UpcomingSortByPreference,
  private val syncTrendingShows: SyncTrendingShows,
  private val syncTrendingMovies: SyncTrendingMovies
) : ViewModel() {

  val upcomingShows: MappedCursorLiveData<List<ShowWithEpisode>>
  val showsWatchlist: LiveData<List<Show>>
  val episodeWatchlist: LiveData<List<Episode>>
  val trendingShows: LiveData<List<Show>>
  val movieWatchlist: LiveData<List<Movie>>
  val trendingMovies: LiveData<List<Movie>>

  private val upcomingSortByListener: UpcomingSortByListener

  init {
    val upcomingSortBy = upcomingSortByPreference.get()

    upcomingShows = MappedCursorLiveData(
      context,
      Shows.SHOWS_UPCOMING,
      ShowWithEpisodeMapper.projection,
      null,
      null,
      upcomingSortBy.sortOrder,
      ShowWithEpisodeListMapper
    )

    showsWatchlist = MappedCursorLiveData(
      context,
      Shows.SHOWS_WATCHLIST,
      ShowMapper.projection,
      null,
      null,
      null,
      ShowListMapper
    )

    episodeWatchlist = MappedCursorLiveData(
      context,
      Episodes.EPISODES_IN_WATCHLIST,
      EpisodeMapper.projection,
      null,
      null,
      null,
      EpisodeListMapper
    )

    trendingShows = MappedCursorLiveData(
      context,
      Shows.SHOWS_TRENDING,
      ShowMapper.projection,
      null,
      null,
      null,
      ShowListMapper
    )

    movieWatchlist = MappedCursorLiveData(
      context,
      Movies.MOVIES_WATCHLIST,
      MovieMapper.projection,
      null,
      null,
      null,
      MovieListMapper
    )

    trendingMovies = MappedCursorLiveData(
      context,
      Movies.TRENDING,
      MovieMapper.projection,
      null,
      null,
      null,
      MovieListMapper
    )

    upcomingSortByListener =
      UpcomingSortByListener { sortBy -> upcomingShows.setSortOrder(sortBy.sortOrder) }
    upcomingSortByPreference.registerListener(upcomingSortByListener)

    viewModelScope.launch {
      if (System.currentTimeMillis() > SuggestionsTimestamps.get(context).getLong(
          SuggestionsTimestamps.SHOWS_TRENDING,
          0L
        ) + TrendingShowsViewModel.SYNC_INTERNAL
      ) {
        syncTrendingShows.invokeAsync(Unit)
      }

      if (System.currentTimeMillis() > SuggestionsTimestamps.get(context).getLong(
          SuggestionsTimestamps.MOVIES_TRENDING,
          0L
        ) + TrendingMoviesViewModel.SYNC_INTERNAL
      ) {
        syncTrendingMovies.invokeAsync(Unit)
      }
    }
  }

  override fun onCleared() {
    upcomingSortByPreference.unregisterListener(upcomingSortByListener)
  }
}
