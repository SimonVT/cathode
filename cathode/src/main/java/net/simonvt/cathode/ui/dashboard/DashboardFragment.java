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
import android.database.Cursor;
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
import javax.inject.Inject;
import net.simonvt.cathode.R;
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
  private Cursor showsWatchlistCursor;
  private Cursor episodeWatchlistCursor;

  private NavigationListener navigationListener;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AndroidSupportInjection.inject(this);

    setTitle(R.string.title_dashboard);

    adapter = new CategoryAdapter(getActivity(), categoryClickListener);
    setAdapter(adapter);

    adapter.initCategory(R.string.category_shows_upcoming);
    adapter.initCategory(R.string.category_shows_watchlist);
    adapter.initCategory(R.string.category_shows_suggestions);
    adapter.initCategory(R.string.category_movies_watchlist);
    adapter.initCategory(R.string.category_movies_suggestions);

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(getActivity(),
        SuggestionsTimestamps.SHOWS_TRENDING)) {
      jobManager.addJob(new SyncTrendingShows());
      SuggestionsTimestamps.updateSuggestions(getActivity(), SuggestionsTimestamps.SHOWS_TRENDING);
    }

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(getActivity(),
        SuggestionsTimestamps.MOVIES_TRENDING)) {
      jobManager.addJob(new SyncTrendingMovies());
      SuggestionsTimestamps.updateSuggestions(getActivity(), SuggestionsTimestamps.MOVIES_TRENDING);
    }

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(DashboardViewModel.class);
    viewModel.getUpcomingShows().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        updateShowsUpcomingCursor(cursor);
      }
    });
    viewModel.getShowsWatchlist().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        updateShowsWatchlistCursor(cursor);
      }
    });
    viewModel.getEpisodeWatchlist().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        updateEpisodeWatchlistCursor(cursor);
      }
    });
    viewModel.getTrendingShows().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        updateShowsTrendingCursor(cursor);
      }
    });
    viewModel.getMovieWatchlist().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        updateMoviesWatchlistCursor(cursor);
      }
    });
    viewModel.getTrendingMovies().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        updateMoviesTrendingCursor(cursor);
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

  private void updateShowsUpcomingCursor(Cursor cursor) {
    if (upcomingShowsAdapter == null) {
      upcomingShowsAdapter = new DashboardUpcomingShowsAdapter(getContext(), callback);
      adapter.setAdapter(R.string.category_shows_upcoming, upcomingShowsAdapter);
    }

    upcomingShowsAdapter.changeCursor(cursor);
  }

  private void updateShowsWatchlistCursor(Cursor cursor) {
    showsWatchlistCursor = cursor;
    if (showsWatchlistAdapter == null) {
      checkWatchlistAdapter();
    } else {
      showsWatchlistAdapter.changeShowsCursor(cursor);
    }
  }

  private void updateEpisodeWatchlistCursor(Cursor cursor) {
    episodeWatchlistCursor = cursor;
    if (showsWatchlistAdapter == null) {
      checkWatchlistAdapter();
    } else {
      showsWatchlistAdapter.changeEpisodeCursor(cursor);
    }
  }

  private void checkWatchlistAdapter() {
    if (showsWatchlistAdapter == null) {
      if (episodeWatchlistCursor != null && showsWatchlistCursor != null) {
        showsWatchlistAdapter = new DashboardShowsWatchlistAdapter(getContext(), callback);
        showsWatchlistAdapter.changeShowsCursor(showsWatchlistCursor);
        showsWatchlistAdapter.changeEpisodeCursor(episodeWatchlistCursor);
        adapter.setAdapter(R.string.category_shows_watchlist, showsWatchlistAdapter);
      }
    }
  }

  private void updateShowsTrendingCursor(Cursor cursor) {
    if (trendingShowsAdapter == null) {
      trendingShowsAdapter = new DashboardShowsAdapter(getContext(), callback);
      adapter.setAdapter(R.string.category_shows_suggestions, trendingShowsAdapter);
    }

    trendingShowsAdapter.changeCursor(cursor);
  }

  private void updateMoviesWatchlistCursor(Cursor cursor) {
    if (movieWatchlistAdapter == null) {
      movieWatchlistAdapter = new DashboardMoviesAdapter(getContext(), callback);
      adapter.setAdapter(R.string.category_movies_watchlist, movieWatchlistAdapter);
    }

    movieWatchlistAdapter.changeCursor(cursor);
  }

  private void updateMoviesTrendingCursor(Cursor cursor) {
    if (trendingMoviesAdapter == null) {
      trendingMoviesAdapter = new DashboardMoviesAdapter(getContext(), callback);
      adapter.setAdapter(R.string.category_movies_suggestions, trendingMoviesAdapter);
    }

    trendingMoviesAdapter.changeCursor(cursor);
  }
}
