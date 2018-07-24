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
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortBy;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference;

public class DashboardViewModel extends AndroidViewModel {

  private CursorLiveData upcomingShows;
  private LiveData<Cursor> showsWatchlist;
  private LiveData<Cursor> episodeWatchlist;
  private LiveData<Cursor> trendingShows;
  private LiveData<Cursor> movieWatchlist;
  private LiveData<Cursor> trendingMovies;

  private UpcomingSortByPreference upcomingSortByPreference;

  public DashboardViewModel(@NonNull Application application,
      UpcomingSortByPreference upcomingSortByPreference) {
    super(application);
    this.upcomingSortByPreference = upcomingSortByPreference;
    upcomingSortByPreference.registerListener(upcomingSortByListener);
    UpcomingSortBy upcomingSortBy = upcomingSortByPreference.get();

    upcomingShows = new CursorLiveData(application, ProviderSchematic.Shows.SHOWS_UPCOMING,
        DashboardUpcomingShowsAdapter.PROJECTION, null, null, upcomingSortBy.getSortOrder());

    showsWatchlist = new CursorLiveData(application, ProviderSchematic.Shows.SHOWS_WATCHLIST,
        DashboardShowsWatchlistAdapter.PROJECTION, null, null, null);

    episodeWatchlist =
        new CursorLiveData(application, ProviderSchematic.Episodes.EPISODES_IN_WATCHLIST,
            DashboardShowsWatchlistAdapter.PROJECTION_EPISODE, null, null, null);

    trendingShows = new CursorLiveData(application, ProviderSchematic.Shows.SHOWS_TRENDING,
        DashboardShowsAdapter.PROJECTION, null, null, null);

    movieWatchlist =
        new CursorLiveData(application, Movies.MOVIES_WATCHLIST, DashboardMoviesAdapter.PROJECTION,
            null, null, null);

    trendingMovies =
        new CursorLiveData(application, Movies.TRENDING, DashboardMoviesAdapter.PROJECTION, null,
            null, null);
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

  public CursorLiveData getUpcomingShows() {
    return upcomingShows;
  }

  public LiveData<Cursor> getShowsWatchlist() {
    return showsWatchlist;
  }

  public LiveData<Cursor> getEpisodeWatchlist() {
    return episodeWatchlist;
  }

  public LiveData<Cursor> getTrendingShows() {
    return trendingShows;
  }

  public LiveData<Cursor> getMovieWatchlist() {
    return movieWatchlist;
  }

  public LiveData<Cursor> getTrendingMovies() {
    return trendingMovies;
  }
}
