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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import net.simonvt.cathode.widget.BottomViewLayout;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.menudrawer.MenuDrawer;
import timber.log.Timber;

public class PhoneController extends UiController {

  @Inject Bus bus;
  @Inject SearchTaskScheduler searchScheduler;

  @Inject ShowTaskScheduler showScheduler;
  @Inject MovieTaskScheduler movieScheduler;

  private FragmentStack stack;

  @InjectView(R.id.drawer) MenuDrawer menuDrawer;

  @InjectView(R.id.mdContent) BottomViewLayout bottomLayout;

  private NavigationFragment navigation;

  private boolean isTablet;

  private Cursor watchingShow;

  private Cursor watchingMovie;

  private static final boolean IS_MATERIAL = Build.VERSION.SDK_INT >= Build.VERSION_CODES.L;

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

    isTablet = activity.getResources().getBoolean(R.bool.isTablet);

    if (!IS_MATERIAL) {
      menuDrawer.setupUpIndicator(activity);
      menuDrawer.setSlideDrawable(R.drawable.ic_drawer);
    }
    menuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_NONE);

    menuDrawer.setOnDrawerStateChangeListener(new MenuDrawer.OnDrawerStateChangeListener() {
      @Override public void onDrawerStateChange(int oldState, int newState) {
        stack.commit();
      }

      @Override public void onDrawerSlide(float openRatio, int offsetPixels) {
      }
    });

    navigation = (NavigationFragment) activity.getSupportFragmentManager()
        .findFragmentByTag(FRAGMENT_NAVIGATION);

    if (navigation == null) {
      navigation = new NavigationFragment();
      activity.getSupportFragmentManager()
          .beginTransaction()
          .add(menuDrawer.getMenuContainer().getId(), navigation, FRAGMENT_NAVIGATION)
          .commit();
    }

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

    menuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_BEZEL);

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
    final int drawerState = menuDrawer.getDrawerState();
    if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
      menuDrawer.closeMenu();
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

    final int drawerState = menuDrawer.getDrawerState();
    if (!stack.pop(drawerState == MenuDrawer.STATE_CLOSED)) {
      menuDrawer.toggleMenu();
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

    menuDrawer.closeMenu();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Watching view
  ///////////////////////////////////////////////////////////////////////////

  private void updateWatching() {
    if (watchingShow != null && watchingShow.moveToFirst()) {
      View watching = LayoutInflater.from(activity).inflate(R.layout.watching_show, null);
      final String show = watchingShow.getString(watchingShow.getColumnIndex(ShowColumns.TITLE));
      final String poster = watchingShow.getString(watchingShow.getColumnIndex(ShowColumns.POSTER));
      final String episode =
          watchingShow.getString(watchingShow.getColumnIndex(EpisodeColumns.TITLE));
      final int season = watchingShow.getInt(watchingShow.getColumnIndex(EpisodeColumns.SEASON));
      final int episodeNumber =
          watchingShow.getInt(watchingShow.getColumnIndex(EpisodeColumns.EPISODE));
      final boolean checkedIn =
          watchingShow.getInt(watchingShow.getColumnIndex(EpisodeColumns.CHECKED_IN)) == 1;

      ((TextView) watching.findViewById(R.id.show)).setText(show);
      ((RemoteImageView) watching.findViewById(R.id.poster)).setImage(poster);
      ((TextView) watching.findViewById(R.id.episode)).setText(
          activity.getString(R.string.episode, season, episodeNumber, episode));
      OverflowView overflow = (OverflowView) watching.findViewById(R.id.overflow);
      if (checkedIn) overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
      overflow.setListener(new OverflowView.OverflowActionListener() {
        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_checkin_cancel:
              showScheduler.cancelCheckin();
              break;
          }
        }
      });

      bottomLayout.setBottomView(watching);
    } else if (watchingMovie != null && watchingMovie.moveToFirst()) {
      View watching = LayoutInflater.from(activity).inflate(R.layout.watching_movie, null);
      final String movie =
          watchingMovie.getString(watchingMovie.getColumnIndex(MovieColumns.TITLE));
      final String poster =
          watchingMovie.getString(watchingMovie.getColumnIndex(MovieColumns.POSTER));
      final String year = watchingMovie.getString(watchingMovie.getColumnIndex(MovieColumns.YEAR));
      final boolean checkedIn =
          watchingMovie.getInt(watchingMovie.getColumnIndex(MovieColumns.CHECKED_IN)) == 1;

      ((TextView) watching.findViewById(R.id.movie)).setText(movie);
      ((RemoteImageView) watching.findViewById(R.id.poster)).setImage(poster);
      ((TextView) watching.findViewById(R.id.year)).setText(year);
      OverflowView overflow = (OverflowView) watching.findViewById(R.id.overflow);
      if (checkedIn) overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
      overflow.setListener(new OverflowView.OverflowActionListener() {
        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_checkin_cancel:
              movieScheduler.cancelCheckin();
              break;
          }
        }
      });

      bottomLayout.setBottomView(watching);
    } else {
      bottomLayout.setBottomView(null);
    }
  }

  private LoaderManager.LoaderCallbacks<Cursor> watchingShowCallback =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
          CursorLoader loader =
              new CursorLoader(activity, Shows.SHOW_WATCHING, null, null, null, null);
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
