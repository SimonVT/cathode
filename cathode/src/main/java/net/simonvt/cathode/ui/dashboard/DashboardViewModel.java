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

package net.simonvt.cathode.ui.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.entity.ShowWithEpisode;
import net.simonvt.cathode.entitymapper.EpisodeListMapper;
import net.simonvt.cathode.entitymapper.MovieListMapper;
import net.simonvt.cathode.entitymapper.ShowListMapper;
import net.simonvt.cathode.entitymapper.ShowWithEpisodeListMapper;
import net.simonvt.cathode.entitymapper.ShowWithEpisodeMapper;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortBy;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference;

public class DashboardViewModel extends AndroidViewModel {

  private MappedCursorLiveData<List<ShowWithEpisode>> upcomingShows;
  private LiveData<List<Show>> showsWatchlist;
  private LiveData<List<Episode>> episodeWatchlist;
  private LiveData<List<Show>> trendingShows;
  private LiveData<List<Movie>> movieWatchlist;
  private LiveData<List<Movie>> trendingMovies;

  private UpcomingSortByPreference upcomingSortByPreference;

  public DashboardViewModel(@NonNull Application application,
      UpcomingSortByPreference upcomingSortByPreference) {
    super(application);
    this.upcomingSortByPreference = upcomingSortByPreference;
    upcomingSortByPreference.registerListener(upcomingSortByListener);
    UpcomingSortBy upcomingSortBy = upcomingSortByPreference.get();

    upcomingShows = new MappedCursorLiveData<>(application, Shows.SHOWS_UPCOMING,
        ShowWithEpisodeMapper.PROJECTION, null, null, upcomingSortBy.getSortOrder(),
        new ShowWithEpisodeListMapper());

    showsWatchlist = new MappedCursorLiveData<>(application, Shows.SHOWS_WATCHLIST,
        DashboardShowsWatchlistAdapter.PROJECTION, null, null, null, new ShowListMapper());

    episodeWatchlist =
        new MappedCursorLiveData<>(application, ProviderSchematic.Episodes.EPISODES_IN_WATCHLIST,
            DashboardShowsWatchlistAdapter.PROJECTION_EPISODE, null, null, null,
            new EpisodeListMapper());

    trendingShows = new MappedCursorLiveData<>(application, Shows.SHOWS_TRENDING,
        DashboardShowsAdapter.PROJECTION, null, null, null, new ShowListMapper());

    movieWatchlist = new MappedCursorLiveData<>(application, Movies.MOVIES_WATCHLIST,
        DashboardMoviesAdapter.PROJECTION, null, null, null, new MovieListMapper());

    trendingMovies =
        new MappedCursorLiveData<>(application, Movies.TRENDING, DashboardMoviesAdapter.PROJECTION,
            null, null, null, new MovieListMapper());
  }

  @Override protected void onCleared() {
    upcomingSortByPreference.unregisterListener(upcomingSortByListener);
  }

  private UpcomingSortByPreference.UpcomingSortByListener upcomingSortByListener =
      new UpcomingSortByPreference.UpcomingSortByListener() {
        @Override public void onUpcomingSortByChanged(UpcomingSortBy sortBy) {
          upcomingShows.setSortOrder(sortBy.getSortOrder());
        }
      };

  public MappedCursorLiveData<List<ShowWithEpisode>> getUpcomingShows() {
    return upcomingShows;
  }

  public LiveData<List<Show>> getShowsWatchlist() {
    return showsWatchlist;
  }

  public LiveData<List<Episode>> getEpisodeWatchlist() {
    return episodeWatchlist;
  }

  public LiveData<List<Show>> getTrendingShows() {
    return trendingShows;
  }

  public LiveData<List<Movie>> getMovieWatchlist() {
    return movieWatchlist;
  }

  public LiveData<List<Movie>> getTrendingMovies() {
    return trendingMovies;
  }

  public UpcomingSortByPreference getUpcomingSortByPreference() {
    return upcomingSortByPreference;
  }
}
