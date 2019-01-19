/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import dagger.android.support.AndroidSupportInjection;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.entity.ShowWithEpisode;
import net.simonvt.cathode.common.ui.adapter.CategoryAdapter;
import net.simonvt.cathode.common.ui.fragment.ToolbarRecyclerFragment;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.movies.SyncTrendingMovies;
import net.simonvt.cathode.remote.sync.shows.SyncTrendingShows;
import net.simonvt.cathode.settings.SuggestionsTimestamps;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment;
import net.simonvt.cathode.ui.shows.watchlist.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.suggestions.movies.MovieSuggestionsFragment;
import net.simonvt.cathode.ui.suggestions.shows.ShowSuggestionsFragment;

public class DashboardFragment extends ToolbarRecyclerFragment<RecyclerView.ViewHolder> {

  public interface OverviewCallback {

    void onDisplayShow(long showId, String title, String overview);

    void onDisplayEpisode(long episodeId, String showTitle);

    void onDisplayMovie(long movieId, String title, String overview);
  }

  public static final String TAG = "net.simonvt.cathode.ui.dashboard.DashboardFragment";

  @Inject JobManager jobManager;

  @Inject DashboardViewModelFactory viewModelFactory;
  private DashboardViewModel viewModel;

  CategoryAdapter adapter;

  DashboardUpcomingShowsAdapter upcomingShowsAdapter;
  DashboardShowsAdapter trendingShowsAdapter;
  DashboardMoviesAdapter movieWatchlistAdapter;
  DashboardMoviesAdapter trendingMoviesAdapter;

  DashboardShowsWatchlistAdapter showsWatchlistAdapter;
  private List<Show> showWatchlist;
  private List<Episode> episodeWatchlist;

  private NavigationListener navigationListener;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AndroidSupportInjection.inject(this);

    setTitle(R.string.title_dashboard);

    adapter = new CategoryAdapter(requireContext(), categoryClickListener);
    setAdapter(adapter);

    adapter.initCategory(R.string.category_shows_upcoming);
    adapter.initCategory(R.string.category_shows_watchlist);
    adapter.initCategory(R.string.category_shows_suggestions);
    adapter.initCategory(R.string.category_movies_watchlist);
    adapter.initCategory(R.string.category_movies_suggestions);

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(requireContext(),
        SuggestionsTimestamps.SHOWS_TRENDING)) {
      jobManager.addJob(new SyncTrendingShows());
      SuggestionsTimestamps.updateSuggestions(requireContext(), SuggestionsTimestamps.SHOWS_TRENDING);
    }

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(requireContext(),
        SuggestionsTimestamps.MOVIES_TRENDING)) {
      jobManager.addJob(new SyncTrendingMovies());
      SuggestionsTimestamps.updateSuggestions(requireContext(), SuggestionsTimestamps.MOVIES_TRENDING);
    }

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(DashboardViewModel.class);
    viewModel.getUpcomingShows().observe(this, new Observer<List<ShowWithEpisode>>() {
      @Override public void onChanged(List<ShowWithEpisode> shows) {
        updateShowsUpcomingCursor(shows);
      }
    });
    viewModel.getShowsWatchlist().observe(this, new Observer<List<Show>>() {
      @Override public void onChanged(List<Show> shows) {
        updateShowsWatchlistCursor(shows);
      }
    });
    viewModel.getEpisodeWatchlist().observe(this, new Observer<List<Episode>>() {
      @Override public void onChanged(List<Episode> episodes) {
        updateEpisodeWatchlistCursor(episodes);
      }
    });
    viewModel.getTrendingShows().observe(this, new Observer<List<Show>>() {
      @Override public void onChanged(List<Show> shows) {
        updateShowsTrendingCursor(shows);
      }
    });
    viewModel.getMovieWatchlist().observe(this, new Observer<List<Movie>>() {
      @Override public void onChanged(List<Movie> movies) {
        updateMoviesWatchlistCursor(movies);
      }
    });
    viewModel.getTrendingMovies().observe(this, new Observer<List<Movie>>() {
      @Override public void onChanged(List<Movie> movies) {
        updateMoviesTrendingCursor(movies);
      }
    });
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_dashboard, container, false);
  }

  @Override public boolean displaysMenuIcon() {
    return true;
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.search);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onSearchClicked();
        return true;
    }

    return super.onMenuItemClick(item);
  }

  private final CategoryAdapter.CategoryClickListener categoryClickListener =
      new CategoryAdapter.CategoryClickListener() {
        @Override public void onCategoryClick(int category) {
          switch (category) {
            case R.string.category_shows_upcoming:
              navigationListener.displayFragment(UpcomingShowsFragment.class,
                  UpcomingShowsFragment.TAG + "_dashboard");
              break;

            case R.string.category_shows_watchlist:
              navigationListener.displayFragment(ShowsWatchlistFragment.class,
                  ShowsWatchlistFragment.TAG + "_dashboard");
              break;

            case R.string.category_shows_suggestions:
              navigationListener.displayFragment(ShowSuggestionsFragment.class,
                  ShowSuggestionsFragment.TAG + "_dashboard");
              break;

            case R.string.category_movies_watchlist:
              navigationListener.displayFragment(MovieWatchlistFragment.class,
                  MovieWatchlistFragment.TAG + "_dashboard");
              break;

            case R.string.category_movies_suggestions:
              navigationListener.displayFragment(MovieSuggestionsFragment.class,
                  MovieSuggestionsFragment.TAG + "_dashboard");
              break;
          }
        }
      };

  private final OverviewCallback callback = new OverviewCallback() {
    @Override public void onDisplayShow(long showId, String title, String overview) {
      navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
    }

    @Override public void onDisplayEpisode(long episodeId, String showTitle) {
      navigationListener.onDisplayEpisode(episodeId, showTitle);
    }

    @Override public void onDisplayMovie(long movieId, String title, String overview) {
      navigationListener.onDisplayMovie(movieId, title, overview);
    }
  };

  private void updateShowsUpcomingCursor(List<ShowWithEpisode> shows) {
    if (upcomingShowsAdapter == null) {
      upcomingShowsAdapter = new DashboardUpcomingShowsAdapter(requireContext(), callback);
      adapter.setAdapter(R.string.category_shows_upcoming, upcomingShowsAdapter);
    }

    upcomingShowsAdapter.setList(shows);
  }

  private void updateShowsWatchlistCursor(List<Show> shows) {
    showWatchlist = shows;
    if (showsWatchlistAdapter == null) {
      checkWatchlistAdapter();
    } else {
      showsWatchlistAdapter.changeShowList(shows);
    }
  }

  private void updateEpisodeWatchlistCursor(List<Episode> episodes) {
    episodeWatchlist = episodes;
    if (showsWatchlistAdapter == null) {
      checkWatchlistAdapter();
    } else {
      showsWatchlistAdapter.changeEpisodeList(episodes);
    }
  }

  private void checkWatchlistAdapter() {
    if (showsWatchlistAdapter == null) {
      if (episodeWatchlist != null && showWatchlist != null) {
        showsWatchlistAdapter = new DashboardShowsWatchlistAdapter(requireContext(), callback);
        showsWatchlistAdapter.changeShowList(showWatchlist);
        showsWatchlistAdapter.changeEpisodeList(episodeWatchlist);
        adapter.setAdapter(R.string.category_shows_watchlist, showsWatchlistAdapter);
      }
    }
  }

  private void updateShowsTrendingCursor(List<Show> shows) {
    if (trendingShowsAdapter == null) {
      trendingShowsAdapter = new DashboardShowsAdapter(requireContext(), callback);
      adapter.setAdapter(R.string.category_shows_suggestions, trendingShowsAdapter);
    }

    trendingShowsAdapter.setList(shows);
  }

  private void updateMoviesWatchlistCursor(List<Movie> movies) {
    if (movieWatchlistAdapter == null) {
      movieWatchlistAdapter = new DashboardMoviesAdapter(requireContext(), callback);
      adapter.setAdapter(R.string.category_movies_watchlist, movieWatchlistAdapter);
    }

    movieWatchlistAdapter.setList(movies);
  }

  private void updateMoviesTrendingCursor(List<Movie> movies) {
    if (trendingMoviesAdapter == null) {
      trendingMoviesAdapter = new DashboardMoviesAdapter(requireContext(), callback);
      adapter.setAdapter(R.string.category_movies_suggestions, trendingMoviesAdapter);
    }

    trendingMoviesAdapter.setList(movies);
  }
}
