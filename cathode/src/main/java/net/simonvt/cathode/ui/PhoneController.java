/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.SearchTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.fragment.EpisodeFragment;
import net.simonvt.cathode.ui.fragment.MovieCollectionFragment;
import net.simonvt.cathode.ui.fragment.MovieFragment;
import net.simonvt.cathode.ui.fragment.MovieRecommendationsFragment;
import net.simonvt.cathode.ui.fragment.MovieWatchlistFragment;
import net.simonvt.cathode.ui.fragment.NavigationFragment;
import net.simonvt.cathode.ui.fragment.SearchMovieFragment;
import net.simonvt.cathode.ui.fragment.SearchShowFragment;
import net.simonvt.cathode.ui.fragment.SeasonFragment;
import net.simonvt.cathode.ui.fragment.ShowFragment;
import net.simonvt.cathode.ui.fragment.ShowRecommendationsFragment;
import net.simonvt.cathode.ui.fragment.ShowsCollectionFragment;
import net.simonvt.cathode.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.fragment.TrendingMoviesFragment;
import net.simonvt.cathode.ui.fragment.TrendingShowsFragment;
import net.simonvt.cathode.ui.fragment.UpcomingShowsFragment;
import net.simonvt.cathode.ui.fragment.WatchedMoviesFragment;
import net.simonvt.cathode.ui.fragment.WatchedShowsFragment;
import net.simonvt.cathode.util.FragmentStack;
import net.simonvt.cathode.widget.WatchingView;
import timber.log.Timber;

import static net.simonvt.cathode.provider.DatabaseSchematic.Tables;

public class PhoneController extends UiController {

  @Inject Bus bus;
  @Inject SearchTaskScheduler searchScheduler;

  @Inject ShowTaskScheduler showScheduler;
  @Inject MovieTaskScheduler movieScheduler;

  private FragmentStack stack;

  @InjectView(R.id.drawer) DrawerLayout drawer;
  private int drawerState = DrawerLayout.STATE_IDLE;

  @InjectView(R.id.watching_parent) ViewGroup watchingParent;
  @InjectView(R.id.watchingView) WatchingView watchingView;

  private NavigationFragment navigation;

  private boolean isTablet;

  private Cursor watchingShow;

  private Cursor watchingMovie;

  private static final boolean IS_MATERIAL = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

  public static PhoneController create(HomeActivity activity, ViewGroup parent) {
    return create(activity, parent, null);
  }

  public static PhoneController create(HomeActivity activity, ViewGroup parent, Bundle inState) {
    return new PhoneController(activity, parent, inState);
  }

  PhoneController(final HomeActivity activity, ViewGroup parent, Bundle inState) {
    super(activity, inState);
    CathodeApp.inject(activity, this);

    LayoutInflater.from(activity).inflate(R.layout.controller_phone, parent);
    ButterKnife.inject(this, activity);
    watchingView.setWatchingViewListener(new WatchingView.WatchingViewListener() {
      @Override public void onExpand(WatchingView view) {
        Timber.d("onExpand");
        watchingParent.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            watchingView.collapse();
          }
        });
      }

      @Override public void onCollapse(WatchingView view) {
        Timber.d("onCollapse");
        watchingParent.setOnClickListener(null);
        watchingParent.setClickable(false);
      }

      @Override public void onEpisodeClicked(WatchingView view, long episodeId, String showTitle) {
        watchingView.collapse();

        Fragment top = stack.peek();
        if (top instanceof EpisodeFragment) {
          EpisodeFragment f = (EpisodeFragment) top;
          if (episodeId == f.getEpisodeId()) {
            return;
          }
        }

        onDisplayEpisode(episodeId, showTitle);
      }

      @Override public void onMovieClicked(WatchingView view, long movieId, String movieTitle) {
        watchingView.collapse();
        onDisplayMovie(movieId, movieTitle);
      }

      @Override public void onAnimatingIn(WatchingView view) {
      }

      @Override public void onAnimatingOut(WatchingView view) {
        watchingParent.setOnClickListener(null);
        watchingParent.setClickable(false);
      }
    });

    isTablet = activity.getResources().getBoolean(R.bool.isTablet);

    drawer.setDrawerListener(new DrawerLayout.DrawerListener() {
      @Override public void onDrawerSlide(View drawerView, float slideOffset) {
      }

      @Override public void onDrawerOpened(View drawerView) {
      }

      @Override public void onDrawerClosed(View drawerView) {
        stack.commit();
      }

      @Override public void onDrawerStateChanged(int newState) {
        drawerState = newState;
      }
    });

    navigation = (NavigationFragment) activity.getSupportFragmentManager()
        .findFragmentByTag(FRAGMENT_NAVIGATION);

    stack =
        FragmentStack.forContainer(activity, R.id.controller_content, new FragmentStack.Callback() {
          @Override public void onStackChanged(int stackSize, Fragment topFragment) {
            Timber.d("onStackChanged: %s", topFragment.getTag());
          }
        });
    stack.setDefaultAnimation(R.anim.fade_in_front, R.anim.fade_out_back, R.anim.fade_in_back,
        R.anim.fade_out_front);

    if (inState != null) {
      stack.restoreState(inState);
    }

    if (stack.size() == 0) {
      stack.replace(UpcomingShowsFragment.class, FRAGMENT_SHOWS_UPCOMING);
      stack.commit();
    }

    activity.getSupportLoaderManager()
        .initLoader(BaseActivity.LOADER_SHOW_WATCHING, null, watchingShowCallback);
    activity.getSupportLoaderManager()
        .initLoader(BaseActivity.LOADER_MOVIE_WATCHING, null, watchingMovieCallback);

    bus.register(this);
  }

  @Override public boolean onBackClicked() {
    if (drawer.isDrawerVisible(Gravity.LEFT)) {
      drawer.closeDrawer(Gravity.LEFT);
      return true;
    }

    final FragmentContract topFragment = (FragmentContract) stack.peek();
    if (topFragment != null && topFragment.onBackPressed()) {
      return true;
    }

    if (stack.pop(true)) {
      return true;
    }

    return false;
  }

  @Override public Bundle onSaveInstanceState() {
    Bundle outState = new Bundle();
    stack.saveState(outState);
    return outState;
  }

  @Override public void destroy(boolean completely) {
    if (completely) {
      stack.destroy();
      activity.getSupportLoaderManager().destroyLoader(BaseActivity.LOADER_SHOW_WATCHING);
      activity.getSupportLoaderManager().destroyLoader(BaseActivity.LOADER_MOVIE_WATCHING);
      activity.getSupportFragmentManager().beginTransaction().remove(navigation).commit();
    }
    bus.unregister(this);
    super.destroy(completely);
  }

  @Override public void onHomeClicked() {
    super.onHomeClicked();

    final boolean drawerVisible = drawer.isDrawerVisible(Gravity.LEFT);
    if (stack.size() == 1) {
      drawer.openDrawer(Gravity.LEFT);
      return;
    }

    if (!stack.pop(!drawerVisible)) {
      if (drawerVisible) {
        drawer.closeDrawer(Gravity.LEFT);
      }
    }
  }

  @Override public void onMenuItemClicked(int id) {
    switch (id) {
      case R.id.menu_shows_upcoming:
        stack.replace(UpcomingShowsFragment.class, FRAGMENT_SHOWS_UPCOMING);
        break;

      case R.id.menu_shows_watched:
        stack.replace(WatchedShowsFragment.class, FRAGMENT_SHOWS);
        break;

      case R.id.menu_shows_collection:
        stack.replace(ShowsCollectionFragment.class, FRAGMENT_SHOWS_COLLECTION);
        break;

      case R.id.menu_shows_watchlist:
        stack.replace(ShowsWatchlistFragment.class, FRAGMENT_SHOWS_WATCHLIST);
        break;

      case R.id.menu_shows_trending:
        stack.replace(TrendingShowsFragment.class, FRAGMENT_SHOWS_TRENDING);
        break;

      case R.id.menu_shows_recommendations:
        stack.replace(ShowRecommendationsFragment.class, FRAGMENT_SHOWS_RECOMMENDATIONS);
        break;

      case R.id.menu_movies_watched:
        stack.replace(WatchedMoviesFragment.class, FRAGMENT_MOVIES_WATCHED);
        break;

      case R.id.menu_movies_collection:
        stack.replace(MovieCollectionFragment.class, FRAGMENT_MOVIES_COLLECTION);
        break;

      case R.id.menu_movies_watchlist:
        stack.replace(MovieWatchlistFragment.class, FRAGMENT_MOVIES_WATCHLIST);
        break;

      case R.id.menu_movies_trending:
        stack.replace(TrendingMoviesFragment.class, FRAGMENT_MOVIES_TRENDING);
        break;

      case R.id.menu_movies_recommendations:
        stack.replace(MovieRecommendationsFragment.class, FRAGMENT_MOVIES_RECOMMENDATIONS);
        break;

      default:
        throw new IllegalArgumentException("Unknown id " + id);
    }

    drawer.closeDrawer(Gravity.LEFT);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Watching view
  ///////////////////////////////////////////////////////////////////////////

  private void updateWatching() {
    if (watchingShow != null && watchingShow.moveToFirst()) {
      final long showId = watchingShow.getLong(watchingShow.getColumnIndex(ShowColumns.ID));
      final String show = watchingShow.getString(watchingShow.getColumnIndex(ShowColumns.TITLE));
      final String poster = watchingShow.getString(watchingShow.getColumnIndex(ShowColumns.POSTER));
      final String episode =
          watchingShow.getString(watchingShow.getColumnIndex(EpisodeColumns.TITLE));
      final int season = watchingShow.getInt(watchingShow.getColumnIndex(EpisodeColumns.SEASON));

      final long episodeId = watchingShow.getLong(watchingShow.getColumnIndex("episodeId"));
      final String episodeTitle =
          watchingShow.getString(watchingShow.getColumnIndex(EpisodeColumns.TITLE));
      final int episodeNumber =
          watchingShow.getInt(watchingShow.getColumnIndex(EpisodeColumns.EPISODE));
      final boolean checkedIn =
          watchingShow.getInt(watchingShow.getColumnIndex(EpisodeColumns.CHECKED_IN)) == 1;
      final long startTime =
          watchingShow.getLong(watchingShow.getColumnIndex(EpisodeColumns.STARTED_AT));
      final long endTime =
          watchingShow.getLong(watchingShow.getColumnIndex(EpisodeColumns.EXPIRES_AT));

      watchingView.watchingShow(showId, show, episodeId, episodeTitle, poster, startTime, endTime);
    } else if (watchingMovie != null && watchingMovie.moveToFirst()) {
      final long id = watchingMovie.getLong(watchingMovie.getColumnIndex(MovieColumns.ID));
      final String movie =
          watchingMovie.getString(watchingMovie.getColumnIndex(MovieColumns.TITLE));
      final String poster =
          watchingMovie.getString(watchingMovie.getColumnIndex(MovieColumns.POSTER));
      final long startTime =
          watchingShow.getLong(watchingShow.getColumnIndex(MovieColumns.STARTED_AT));
      final long endTime =
          watchingShow.getLong(watchingShow.getColumnIndex(MovieColumns.EXPIRES_AT));

      watchingView.watchingMovie(id, movie, poster, startTime, endTime);
    } else {
      watchingView.clearWatching();
    }
  }

  private static final String[] SHOW_WATCHING_PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.POSTER,
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS episodeId",
      Tables.EPISODES + "." + EpisodeColumns.TITLE, Tables.EPISODES + "." + EpisodeColumns.SEASON,
      Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.CHECKED_IN,
      Tables.EPISODES + "." + EpisodeColumns.STARTED_AT,
      Tables.EPISODES + "." + EpisodeColumns.EXPIRES_AT,
  };

  private LoaderManager.LoaderCallbacks<Cursor> watchingShowCallback =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
          CursorLoader loader =
              new CursorLoader(activity, Shows.SHOW_WATCHING, SHOW_WATCHING_PROJECTION, null, null,
                  null);
          loader.setUpdateThrottle(2000);
          return loader;
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
          watchingShow = cursor;
          updateWatching();
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
          watchingShow = null;
        }
      };

  private LoaderManager.LoaderCallbacks<Cursor> watchingMovieCallback =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
          CursorLoader loader =
              new CursorLoader(activity, Movies.WATCHING, null, MovieColumns.NEEDS_SYNC + "=0",
                  null, null);
          loader.setUpdateThrottle(2000);
          return loader;
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
          watchingMovie = cursor;
          updateWatching();
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
          watchingMovie = null;
        }
      };

  ///////////////////////////////////////////////////////////////////////////
  // Navigation callbacks
  ///////////////////////////////////////////////////////////////////////////

  @Override public void onDisplayShow(long showId, String title, LibraryType type) {
    stack.push(ShowFragment.class, FRAGMENT_SHOW, ShowFragment.getArgs(showId, title, type));
    stack.commit();
  }

  @Override public void onDisplayEpisode(long episodeId, String showTitle) {
    Bundle args = EpisodeFragment.getArgs(episodeId, showTitle);
    if (isTablet) {
      EpisodeFragment f =
          (EpisodeFragment) Fragment.instantiate(activity, EpisodeFragment.class.getName(), args);
      f.show(activity.getSupportFragmentManager(), FRAGMENT_EPISODE);
    } else {
      stack.push(EpisodeFragment.class, FRAGMENT_EPISODE,
          EpisodeFragment.getArgs(episodeId, showTitle));
      stack.commit();
    }
  }

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    stack.push(SeasonFragment.class, FRAGMENT_SEASON,
        SeasonFragment.getArgs(showId, seasonId, showTitle, seasonNumber, type));
    stack.commit();
  }

  @Override public void searchShow(String query) {
    super.searchShow(query);

    SearchShowFragment f = (SearchShowFragment) activity.getSupportFragmentManager()
        .findFragmentByTag(FRAGMENT_SEARCH_SHOW);
    if (f == null) {
      stack.push(SearchShowFragment.class, FRAGMENT_SEARCH_SHOW, SearchShowFragment.getArgs(query));
      stack.executePendingTransactions();
    } else {
      f.query(query);
    }
  }

  @Override public void onDisplayMovie(long movieId, String title) {
    stack.push(MovieFragment.class, FRAGMENT_MOVIE, MovieFragment.getArgs(movieId, title));
    stack.commit();
  }

  @Override public void searchMovie(String query) {
    super.searchMovie(query);

    SearchMovieFragment f = (SearchMovieFragment) activity.getSupportFragmentManager()
        .findFragmentByTag(FRAGMENT_SEARCH_MOVIE);
    if (f == null) {
      stack.push(SearchMovieFragment.class, FRAGMENT_SEARCH_MOVIE,
          SearchMovieFragment.getArgs(query));
      stack.executePendingTransactions();
    } else {
      f.query(query);
    }
  }
}
