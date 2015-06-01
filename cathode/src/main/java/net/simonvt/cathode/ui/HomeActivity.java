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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ProgressBar;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.event.LogoutEvent;
import net.simonvt.cathode.event.MessageEvent;
import net.simonvt.cathode.event.RequestFailedEvent;
import net.simonvt.cathode.jobqueue.SyncEvent;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.dialog.LogoutDialog;
import net.simonvt.cathode.ui.fragment.ActorsFragment;
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
import net.simonvt.cathode.widget.Crouton;
import net.simonvt.cathode.widget.WatchingView;
import net.simonvt.cathode.widget.WatchingView.WatchingViewListener;
import net.simonvt.messagebar.MessageBar;
import timber.log.Timber;

public class HomeActivity extends BaseActivity
    implements NavigationFragment.OnMenuClickListener, ShowsNavigationListener,
    MoviesNavigationListener {

  public static final String DIALOG_ABOUT = "net.simonvt.cathode.ui.BaseActivity.aboutDialog";
  static final String FRAGMENT_NAVIGATION =
      "net.simonvt.cathode.ui.HomeActivity.navigationFragment";
  static final String FRAGMENT_SHOWS = "net.simonvt.cathode.ui.HomeActivity.showsFragment";
  static final String FRAGMENT_SHOWS_UPCOMING =
      "net.simonvt.cathode.ui.HomeActivity.upcomingShowsFragment";
  static final String FRAGMENT_SHOWS_COLLECTION =
      "net.simonvt.cathode.ui.HomeActivity.collectionShowsFragment";
  static final String FRAGMENT_SHOWS_TRENDING =
      "net.simonvt.cathode.ui.HomeActivity.trendingShowsFragment";
  static final String FRAGMENT_SHOWS_RECOMMENDATIONS =
      "net.simonvt.cathode.ui.HomeActivity.showRecommendationsFragment";
  static final String FRAGMENT_SHOW = "net.simonvt.cathode.ui.HomeActivity.showFragment";
  static final String FRAGMENT_SEASON = "net.simonvt.cathode.ui.HomeActivity.seasonFragment";
  static final String FRAGMENT_EPISODE = "net.simonvt.cathode.ui.HomeActivity.episodeFragment";
  static final String FRAGMENT_SHOWS_WATCHLIST =
      "net.simonvt.cathode.ui.HomeActivity.showsWatchlistFragment";
  static final String FRAGMENT_SEARCH_SHOW =
      "net.simonvt.cathode.ui.HomeActivity.searchShowFragment";
  static final String FRAGMENT_MOVIES_WATCHED =
      "net.simonvt.cathode.ui.HomeActivity.moviesWatchedFragment";
  static final String FRAGMENT_MOVIES_COLLECTION =
      "net.simonvt.cathode.ui.HomeActivity.moviesCollectionFragment";
  static final String FRAGMENT_MOVIES_WATCHLIST =
      "net.simonvt.cathode.ui.HomeActivity.moviesWatchlistFragment";
  static final String FRAGMENT_MOVIES_TRENDING =
      "net.simonvt.cathode.ui.HomeActivity.moviesTrendingFragment";
  static final String FRAGMENT_MOVIES_RECOMMENDATIONS =
      "net.simonvt.cathode.ui.HomeActivity.movieRecommendationsFragment";
  static final String FRAGMENT_SEARCH_MOVIE =
      "net.simonvt.cathode.ui.HomeActivity.searchMovieFragment";
  static final String FRAGMENT_MOVIE = "net.simonvt.cathode.ui.HomeActivity.movieFragment";
  static final String FRAGMENT_ACTORS = "net.simonvt.cathode.ui.HomeActivity.actorsFragment";

  private static final String STATE_STACK = "net.simonvt.cathode.ui.HomeActivity.stack";

  public static final String ACTION_LOGIN = "net.simonvt.cathode.intent.action.LOGIN";
  public static final String DIALOG_LOGOUT = "net.simonvt.cathode.ui.HomeActivity.logoutDialog";

  @Inject Bus bus;

  protected MessageBar messageBar;

  @InjectView(R.id.progress_top) ProgressBar progressTop;

  @InjectView(R.id.crouton) Crouton crouton;

  private FragmentStack stack;

  @InjectView(R.id.drawer) DrawerLayout drawer;
  private int drawerState = DrawerLayout.STATE_IDLE;
  private NavigationFragment navigation;

  @InjectView(R.id.watching_parent) ViewGroup watchingParent;
  @InjectView(R.id.watchingView) WatchingView watchingView;

  private Cursor watchingShow;
  private Cursor watchingMovie;

  private boolean isTablet;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    Timber.d("onCreate");
    CathodeApp.inject(this);

    setContentView(R.layout.activity_home);

    ButterKnife.inject(this);

    isTablet = getResources().getBoolean(R.bool.isTablet);

    navigation =
        (NavigationFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_NAVIGATION);

    watchingParent.setOnTouchListener(new View.OnTouchListener() {
      @Override public boolean onTouch(View v, MotionEvent event) {
        if (watchingView.isExpanded()) {
          final int action = event.getActionMasked();
          if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            watchingView.collapse();
          }

          return true;
        }

        return false;
      }
    });
    watchingView.setWatchingViewListener(watchingListener);

    stack = FragmentStack.forContainer(this, R.id.content, new FragmentStack.Callback() {
      @Override public void onStackChanged(int stackSize, Fragment topFragment) {
        Timber.d("onStackChanged: %s", topFragment.getTag());
      }
    });
    stack.setDefaultAnimation(R.anim.fade_in_front, R.anim.fade_out_back, R.anim.fade_in_back,
        R.anim.fade_out_front);
    if (inState != null) {
      stack.restoreState(inState.getBundle(STATE_STACK));
    }
    if (stack.size() == 0) {
      stack.replace(UpcomingShowsFragment.class, FRAGMENT_SHOWS_UPCOMING);
      stack.commit();
    }

    getSupportLoaderManager().initLoader(Loaders.LOADER_SHOW_WATCHING, null, watchingShowCallback);
    getSupportLoaderManager().initLoader(Loaders.LOADER_MOVIE_WATCHING, null,
        watchingMovieCallback);

    messageBar = new MessageBar(this);

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

    if (!Settings.isLoggedIn(this) || isLoginAction(getIntent())) {
      startLoginActivity();
    }
  }

  @Override protected void onNewIntent(Intent intent) {
    if (isLoginAction(intent)) {
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override public void run() {
          startLoginActivity();
        }
      });
    }
  }

  private boolean isLoginAction(Intent intent) {
    return ACTION_LOGIN.equals(intent.getAction());
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    outState.putBundle(STATE_STACK, stack.saveState());
    super.onSaveInstanceState(outState);
  }

  @Override protected void onResume() {
    super.onResume();
    bus.register(this);
  }

  @Override protected void onPause() {
    bus.unregister(this);
    stack.commit();
    super.onPause();
  }

  @Override protected void onDestroy() {
    Timber.d("onDestroy");
    super.onDestroy();
  }

  @Override public void onBackPressed() {
    if (watchingView.isExpanded()) {
      watchingView.collapse();
      return;
    }

    if (drawer.isDrawerVisible(Gravity.LEFT)) {
      drawer.closeDrawer(Gravity.LEFT);
      return;
    }

    final FragmentContract topFragment = (FragmentContract) stack.peek();
    if (topFragment != null && topFragment.onBackPressed()) {
      return;
    }

    if (stack.pop(true)) {
      return;
    }

    super.onBackPressed();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onHomeClicked();
        return true;

      case R.id.menu_logout:
        new LogoutDialog().show(getSupportFragmentManager(), DIALOG_LOGOUT);
        return true;
    }

    return super.onOptionsItemSelected(item);
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

  private WatchingViewListener watchingListener = new WatchingViewListener() {
    @Override public void onExpand(WatchingView view) {
      Timber.d("onExpand");
    }

    @Override public void onCollapse(WatchingView view) {
      Timber.d("onCollapse");
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
    }
  };

  @Subscribe public void onSyncEvent(SyncEvent event) {
    final int progressVisibility = progressTop.getVisibility();
    ViewPropertyAnimator progressAnimator = progressTop.animate();
    if (event.isSyncing()) {
      if (progressVisibility == View.GONE) {
        progressTop.setAlpha(0.0f);
        progressTop.setVisibility(View.VISIBLE);
      }

      progressAnimator.alpha(1.0f);
    } else {
      progressAnimator.alpha(0.0f).withEndAction(new Runnable() {
        @Override public void run() {
          progressTop.setVisibility(View.GONE);
        }
      });
    }
  }

  @Subscribe public void onShowMessage(MessageEvent event) {
    if (event.getMessage() != null) {
      messageBar.show(event.getMessage());
    } else {
      messageBar.show(getString(event.getMessageRes()));
    }
  }

  @Subscribe public void onRequestFailedEvent(RequestFailedEvent event) {
    crouton.show(getString(event.getErrorMessage()),
        getResources().getColor(android.R.color.holo_red_dark));
  }

  @Subscribe public void onLogout(LogoutEvent event) {
    startLoginActivity();
  }

  private void startLoginActivity() {
    Intent login = new Intent(this, LoginActivity.class);
    startActivity(login);
    finish();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Navigation callbacks
  ///////////////////////////////////////////////////////////////////////////

  @Override public void onHomeClicked() {
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

  @Override public void onDisplayShow(long showId, String title, LibraryType type) {
    stack.push(ShowFragment.class, FRAGMENT_SHOW, ShowFragment.getArgs(showId, title, type));
    stack.commit();
  }

  @Override public void onDisplayEpisode(long episodeId, String showTitle) {
    Bundle args = EpisodeFragment.getArgs(episodeId, showTitle);
    if (isTablet) {
      EpisodeFragment f =
          (EpisodeFragment) Fragment.instantiate(this, EpisodeFragment.class.getName(), args);
      f.show(getSupportFragmentManager(), FRAGMENT_EPISODE);
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

    SearchShowFragment f =
        (SearchShowFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_SEARCH_SHOW);
    if (f == null) {
      stack.push(SearchShowFragment.class, FRAGMENT_SEARCH_SHOW, SearchShowFragment.getArgs(query));
      stack.commit();
    } else {
      f.query(query);
    }
  }

  @Override public void onDisplayShowActors(long showId, String title) {
    stack.push(ActorsFragment.class, FRAGMENT_ACTORS, ActorsFragment.forShow(showId, title));
    stack.commit();
  }

  @Override public void onDisplayMovie(long movieId, String title) {
    stack.push(MovieFragment.class, FRAGMENT_MOVIE, MovieFragment.getArgs(movieId, title));
    stack.commit();
  }

  @Override public void searchMovie(String query) {

    SearchMovieFragment f =
        (SearchMovieFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_SEARCH_MOVIE);
    if (f == null) {
      stack.push(SearchMovieFragment.class, FRAGMENT_SEARCH_MOVIE,
          SearchMovieFragment.getArgs(query));
      stack.commit();
    } else {
      f.query(query);
    }
  }

  @Override public void onDisplayMovieActors(long movieId, String title) {
    stack.push(ActorsFragment.class, FRAGMENT_ACTORS, ActorsFragment.forMovie(movieId, title));
    stack.commit();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Watching view
  ///////////////////////////////////////////////////////////////////////////

  private void updateWatching() {
    if (watchingShow != null && watchingShow.moveToFirst()) {
      final long showId =
          watchingShow.getLong(watchingShow.getColumnIndex(DatabaseContract.ShowColumns.ID));
      final String show =
          watchingShow.getString(watchingShow.getColumnIndex(DatabaseContract.ShowColumns.TITLE));
      final String poster =
          watchingShow.getString(watchingShow.getColumnIndex(DatabaseContract.ShowColumns.POSTER));
      final String episode = watchingShow.getString(
          watchingShow.getColumnIndex(DatabaseContract.EpisodeColumns.TITLE));
      final int season =
          watchingShow.getInt(watchingShow.getColumnIndex(DatabaseContract.EpisodeColumns.SEASON));

      final long episodeId = watchingShow.getLong(watchingShow.getColumnIndex("episodeId"));
      final String episodeTitle = watchingShow.getString(
          watchingShow.getColumnIndex(DatabaseContract.EpisodeColumns.TITLE));
      final int episodeNumber =
          watchingShow.getInt(watchingShow.getColumnIndex(DatabaseContract.EpisodeColumns.EPISODE));
      final boolean checkedIn = watchingShow.getInt(
          watchingShow.getColumnIndex(DatabaseContract.EpisodeColumns.CHECKED_IN)) == 1;
      final long startTime = watchingShow.getLong(
          watchingShow.getColumnIndex(DatabaseContract.EpisodeColumns.STARTED_AT));
      final long endTime = watchingShow.getLong(
          watchingShow.getColumnIndex(DatabaseContract.EpisodeColumns.EXPIRES_AT));

      watchingView.watchingShow(showId, show, episodeId, episodeTitle, poster, startTime, endTime);
    } else if (watchingMovie != null && watchingMovie.moveToFirst()) {
      final long id =
          watchingMovie.getLong(watchingMovie.getColumnIndex(DatabaseContract.MovieColumns.ID));
      final String movie = watchingMovie.getString(
          watchingMovie.getColumnIndex(DatabaseContract.MovieColumns.TITLE));
      final String poster = watchingMovie.getString(
          watchingMovie.getColumnIndex(DatabaseContract.MovieColumns.POSTER));
      final long startTime = watchingMovie.getLong(
          watchingMovie.getColumnIndex(DatabaseContract.MovieColumns.STARTED_AT));
      final long endTime = watchingMovie.getLong(
          watchingMovie.getColumnIndex(DatabaseContract.MovieColumns.EXPIRES_AT));

      watchingView.watchingMovie(id, movie, poster, startTime, endTime);
    } else {
      watchingView.clearWatching();
    }
  }

  private static final String[] SHOW_WATCHING_PROJECTION = new String[] {
      DatabaseSchematic.Tables.SHOWS + "." + DatabaseContract.ShowColumns.ID,
      DatabaseSchematic.Tables.SHOWS + "." + DatabaseContract.ShowColumns.TITLE,
      DatabaseSchematic.Tables.SHOWS + "." + DatabaseContract.ShowColumns.POSTER,
      DatabaseSchematic.Tables.EPISODES
          + "."
          + DatabaseContract.EpisodeColumns.ID
          + " AS episodeId",
      DatabaseSchematic.Tables.EPISODES + "." + DatabaseContract.EpisodeColumns.TITLE,
      DatabaseSchematic.Tables.EPISODES + "." + DatabaseContract.EpisodeColumns.SEASON,
      DatabaseSchematic.Tables.EPISODES + "." + DatabaseContract.EpisodeColumns.EPISODE,
      DatabaseSchematic.Tables.EPISODES + "." + DatabaseContract.EpisodeColumns.CHECKED_IN,
      DatabaseSchematic.Tables.EPISODES + "." + DatabaseContract.EpisodeColumns.STARTED_AT,
      DatabaseSchematic.Tables.EPISODES + "." + DatabaseContract.EpisodeColumns.EXPIRES_AT,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> watchingShowCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
          SimpleCursorLoader loader =
              new SimpleCursorLoader(HomeActivity.this, ProviderSchematic.Shows.SHOW_WATCHING,
                  SHOW_WATCHING_PROJECTION, null, null, null);
          loader.setUpdateThrottle(2000);
          return loader;
        }

        @Override
        public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor cursor) {
          watchingShow = cursor;
          updateWatching();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
          watchingShow = null;
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleCursor> watchingMovieCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
          SimpleCursorLoader loader =
              new SimpleCursorLoader(HomeActivity.this, ProviderSchematic.Movies.WATCHING, null,
                  DatabaseContract.MovieColumns.NEEDS_SYNC + "=0", null, null);
          loader.setUpdateThrottle(2000);
          return loader;
        }

        @Override
        public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor cursor) {
          watchingMovie = cursor;
          updateWatching();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
          watchingMovie = null;
        }
      };
}
