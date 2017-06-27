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
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.sync.movies.SyncTrendingMovies;
import net.simonvt.cathode.remote.sync.shows.SyncTrendingShows;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.adapter.CategoryAdapter;
import net.simonvt.cathode.ui.fragment.ToolbarRecyclerFragment;
import net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortBy;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference;
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

  private static final int LOADER_SHOWS_UPCOMING = 1;
  private static final int LOADER_SHOWS_WATCHLIST = 2;
  private static final int LOADER_EPISODES_WATCHLIST = 3;
  private static final int LOADER_SHOWS_TRENDING = 4;
  private static final int LOADER_MOVIES_WATCHLIST = 5;
  private static final int LOADER_MOVIES_TRENDING = 6;

  @Inject JobManager jobManager;

  CategoryAdapter adapter;

  DashboardUpcomingShowsAdapter upcomingShowsAdapter;
  UpcomingSortBy upcomingSortBy;
  @Inject UpcomingSortByPreference upcomingSortByPreference;

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
    Injector.obtain().inject(this);

    setTitle(R.string.title_dashboard);

    upcomingSortByPreference.registerListener(upcomingSortByListener);
    upcomingSortBy = upcomingSortByPreference.get();

    getLoaderManager().initLoader(LOADER_SHOWS_UPCOMING, null, showsUpcomingCallback);
    getLoaderManager().initLoader(LOADER_SHOWS_WATCHLIST, null, showsWatchlistCallback);
    getLoaderManager().initLoader(LOADER_EPISODES_WATCHLIST, null, episodeWatchlistCallback);
    getLoaderManager().initLoader(LOADER_SHOWS_TRENDING, null, showsTrendingCallback);
    getLoaderManager().initLoader(LOADER_MOVIES_WATCHLIST, null, moviesWatchlistCallback);
    getLoaderManager().initLoader(LOADER_MOVIES_TRENDING, null, moviesTrendingCallback);

    adapter = new CategoryAdapter(getActivity(), categoryClickListener);
    setAdapter(adapter);

    adapter.initCategory(R.string.category_shows_upcoming);
    adapter.initCategory(R.string.category_shows_watchlist);
    adapter.initCategory(R.string.category_shows_suggestions);
    adapter.initCategory(R.string.category_movies_watchlist);
    adapter.initCategory(R.string.category_movies_suggestions);

    if (TraktTimestamps.suggestionsNeedsUpdate(getActivity(),
        Settings.SUGGESTIONS_SHOWS_TRENDING)) {
      jobManager.addJob(new SyncTrendingShows());
      TraktTimestamps.updateSuggestions(getActivity(), Settings.SUGGESTIONS_SHOWS_TRENDING);
    }

    if (TraktTimestamps.suggestionsNeedsUpdate(getActivity(),
        Settings.SUGGESTIONS_MOVIES_TRENDING)) {
      jobManager.addJob(new SyncTrendingMovies());
      TraktTimestamps.updateSuggestions(getActivity(), Settings.SUGGESTIONS_MOVIES_TRENDING);
    }
  }

  @Override public void onDestroy() {
    upcomingSortByPreference.unregisterListener(upcomingSortByListener);
    super.onDestroy();
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

  private UpcomingSortByPreference.UpcomingSortByListener upcomingSortByListener =
      new UpcomingSortByPreference.UpcomingSortByListener() {
        @Override public void onUpcomingSortByChanged(UpcomingSortBy sortBy) {
          DashboardFragment.this.upcomingSortBy = sortBy;
          getLoaderManager().restartLoader(LOADER_SHOWS_UPCOMING, null, showsUpcomingCallback);
        }
      };

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

  private void updateShowsUpcomingCursor(SimpleCursor cursor) {
    if (upcomingShowsAdapter == null) {
      upcomingShowsAdapter = new DashboardUpcomingShowsAdapter(getContext(), callback);
      adapter.setAdapter(R.string.category_shows_upcoming, upcomingShowsAdapter);
    }

    upcomingShowsAdapter.changeCursor(cursor);
  }

  private void updateShowsWatchlistCursor(SimpleCursor cursor) {
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

  private void updateShowsTrendingCursor(SimpleCursor cursor) {
    if (trendingShowsAdapter == null) {
      trendingShowsAdapter = new DashboardShowsAdapter(getContext(), callback);
      adapter.setAdapter(R.string.category_shows_suggestions, trendingShowsAdapter);
    }

    trendingShowsAdapter.changeCursor(cursor);
  }

  private void updateMoviesWatchlistCursor(SimpleCursor cursor) {
    if (movieWatchlistAdapter == null) {
      movieWatchlistAdapter = new DashboardMoviesAdapter(getContext(), callback);
      adapter.setAdapter(R.string.category_movies_watchlist, movieWatchlistAdapter);
    }

    movieWatchlistAdapter.changeCursor(cursor);
  }

  private void updateMoviesTrendingCursor(SimpleCursor cursor) {
    if (trendingMoviesAdapter == null) {
      trendingMoviesAdapter = new DashboardMoviesAdapter(getContext(), callback);
      adapter.setAdapter(R.string.category_movies_suggestions, trendingMoviesAdapter);
    }

    trendingMoviesAdapter.changeCursor(cursor);
  }

  private final LoaderManager.LoaderCallbacks<SimpleCursor> showsUpcomingCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Shows.SHOWS_UPCOMING,
              DashboardUpcomingShowsAdapter.PROJECTION, null, null, upcomingSortBy.getSortOrder());
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateShowsUpcomingCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private final LoaderManager.LoaderCallbacks<SimpleCursor> showsWatchlistCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Shows.SHOWS_WATCHLIST,
              DashboardShowsWatchlistAdapter.PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateShowsWatchlistCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private final LoaderManager.LoaderCallbacks<SimpleCursor> episodeWatchlistCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Episodes.EPISODES_IN_WATCHLIST,
              DashboardShowsWatchlistAdapter.PROJECTION_EPISODE, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateEpisodeWatchlistCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private final LoaderManager.LoaderCallbacks<SimpleCursor> showsTrendingCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Shows.SHOWS_TRENDING,
              DashboardShowsAdapter.PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateShowsTrendingCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private final LoaderManager.LoaderCallbacks<SimpleCursor> moviesWatchlistCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), ProviderSchematic.Movies.MOVIES_WATCHLIST,
              DashboardMoviesAdapter.PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateMoviesWatchlistCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private final LoaderManager.LoaderCallbacks<SimpleCursor> moviesTrendingCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), ProviderSchematic.Movies.TRENDING,
              DashboardMoviesAdapter.PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateMoviesTrendingCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
