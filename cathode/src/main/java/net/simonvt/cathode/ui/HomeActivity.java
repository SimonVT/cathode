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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.LoginEvent;
import net.simonvt.cathode.event.LogoutEvent;
import net.simonvt.cathode.event.MessageEvent;
import net.simonvt.cathode.event.SyncEvent;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncUserActivityTask;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.dialog.LogoutDialog;
import net.simonvt.cathode.ui.fragment.NavigationFragment;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.widget.BottomViewLayout;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.messagebar.MessageBar;

public class HomeActivity extends BaseActivity
    implements NavigationFragment.OnMenuClickListener, ShowsNavigationListener,
    MoviesNavigationListener {

  private static final String TAG = "HomeActivity";

  private static final String STATE_LOGIN_CONTROLLER =
      "net.simonvt.cathode.ui.HomeActivity.loginController";
  private static final String STATE_UICONTROLLER =
      "net.simonvt.cathode.ui.HomeActivity.uiController";
  private static final String STATE_LAST_SYNC = "net.simonvt.cathode.ui.HomeActivity.lastSync";

  public static final String ACTION_LOGIN = "net.simonvt.cathode.intent.action.LOGIN";
  public static final String DIALOG_LOGOUT = "net.simonvt.cathode.ui.HomeActivity.logoutDialog";

  private static final long SYNC_DELAY = 15 * DateUtils.MINUTE_IN_MILLIS;

  @Inject TraktTaskQueue queue;

  @Inject Bus bus;

  @Inject ShowTaskScheduler showScheduler;
  @Inject MovieTaskScheduler movieScheduler;

  protected MessageBar messageBar;

  @InjectView(R.id.progress_top) ProgressBar progressTop;
  private ViewPropertyAnimator progressAnimator;

  @InjectView(R.id.bottomLayout) BottomViewLayout bottomLayout;

  private LoginController loginController;

  private UiController uiController;

  private UiController activeController;

  private long lastSync;

  private Handler syncHandler;

  private Runnable syncRunnable = new Runnable() {
    @Override public void run() {
      queue.add(new SyncUserActivityTask());
      lastSync = System.currentTimeMillis();
      syncHandler.postDelayed(this, SYNC_DELAY);
    }
  };

  private Cursor watchingShow;

  private Cursor watchingMovie;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.ui_content_view);

    ButterKnife.inject(this);

    messageBar = new MessageBar(this);

    if (!CathodeApp.accountExists(this) || isLoginAction(getIntent())) {
      Bundle loginState = inState != null ? inState.getBundle(STATE_LOGIN_CONTROLLER) : null;
      loginController = LoginController.newInstance(this, loginState);

      activeController = loginController;
    } else {
      Bundle uiState = inState != null ? inState.getBundle(STATE_UICONTROLLER) : null;
      uiController = PhoneController.newInstance(this, uiState);
      activeController = uiController;

      getSupportLoaderManager().initLoader(LOADER_SHOW_WATCHING, null, watchingShowCallback);
      getSupportLoaderManager().initLoader(LOADER_MOVIE_WATCHING, null, watchingMovieCallback);
    }

    syncHandler = new Handler();

    if (inState != null) {
      lastSync = inState.getLong(STATE_LAST_SYNC);
    }
  }

  @Override protected void onNewIntent(Intent intent) {
    if (isLoginAction(intent)) {
      if (uiController != null) {
        uiController.destroy(true);
        uiController = null;

        loginController = LoginController.newInstance(this);
        activeController = loginController;
      }
    }
  }

  private boolean isLoginAction(Intent intent) {
    return ACTION_LOGIN.equals(intent.getAction());
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (loginController != null) {
      outState.putBundle(STATE_LOGIN_CONTROLLER, loginController.onSaveInstanceState());
    }
    if (uiController != null) {
      outState.putBundle(STATE_UICONTROLLER, uiController.onSaveInstanceState());
    }
    outState.putLong(STATE_LAST_SYNC, lastSync);
  }

  @Override protected void onResume() {
    super.onResume();
    bus.register(this);

    if (uiController != null) {
      if (lastSync + SYNC_DELAY < System.currentTimeMillis()) {
        syncRunnable.run();
      } else {
        syncHandler.postDelayed(syncRunnable, SYNC_DELAY);
      }
    }
  }

  @Override protected void onPause() {
    syncHandler.removeCallbacks(syncRunnable);
    bus.unregister(this);
    super.onPause();
  }

  @Override protected void onDestroy() {
    if (uiController != null) {
      uiController.destroy(false);
    }
    super.onDestroy();
  }

  @Override public void onBackPressed() {
    if (!activeController.onBackClicked()) {
      super.onBackPressed();
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        activeController.onHomeClicked();
        return true;

      case R.id.menu_logout:
        new LogoutDialog().show(getSupportFragmentManager(), DIALOG_LOGOUT);
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private void updateWatching() {
    if (watchingShow != null && watchingShow.moveToFirst()) {
      View watching = LayoutInflater.from(this).inflate(R.layout.watching_show, null);
      final String show =
          watchingShow.getString(watchingShow.getColumnIndex(CathodeContract.Shows.TITLE));
      final String poster =
          watchingShow.getString(watchingShow.getColumnIndex(CathodeContract.Shows.POSTER));
      final String episode =
          watchingShow.getString(watchingShow.getColumnIndex(CathodeContract.Episodes.TITLE));
      final int season = watchingShow.getInt(
          watchingShow.getColumnIndex(CathodeContract.Episodes.SEASON));
      final int episodeNumber = watchingShow.getInt(
          watchingShow.getColumnIndex(CathodeContract.Episodes.EPISODE));

      ((TextView) watching.findViewById(R.id.show)).setText(show);
      ((RemoteImageView) watching.findViewById(R.id.poster)).setImage(poster);
      ((TextView) watching.findViewById(R.id.episode)).setText(
          getString(R.string.episode, season, episodeNumber, episode));
      OverflowView overflow = (OverflowView) watching.findViewById(R.id.overflow);
      overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
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
      View watching = LayoutInflater.from(this).inflate(R.layout.watching_movie, null);
      final String movie =
          watchingMovie.getString(watchingMovie.getColumnIndex(CathodeContract.Movies.TITLE));
      final String poster =
          watchingMovie.getString(watchingMovie.getColumnIndex(CathodeContract.Movies.POSTER));
      final String year =
          watchingMovie.getString(watchingMovie.getColumnIndex(CathodeContract.Movies.YEAR));

      ((TextView) watching.findViewById(R.id.movie)).setText(movie);
      ((RemoteImageView) watching.findViewById(R.id.poster)).setImage(poster);
      ((TextView) watching.findViewById(R.id.year)).setText(year);
      OverflowView overflow = (OverflowView) watching.findViewById(R.id.overflow);
      overflow.addItem(R.id.action_checkin_cancel, R.string.action_checkin_cancel);
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
              new CursorLoader(HomeActivity.this, CathodeContract.Shows.SHOW_WATCHING, null, null,
                  null, null);
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
              new CursorLoader(HomeActivity.this, CathodeContract.Movies.MOVIE_WATCHING, null, null,
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

  @Override public void onMenuItemClicked(int id) {
    activeController.onMenuItemClicked(id);
  }

  @Subscribe public void onSyncEvent(SyncEvent event) {
    final int progressVisibility = progressTop.getVisibility();
    progressAnimator = progressTop.animate();
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

  @Subscribe public void onAuthFailed(AuthFailedEvent event) {
  }

  @Subscribe public void onLogin(LoginEvent event) {
    loginController.destroy(true);
    loginController = null;

    uiController = PhoneController.newInstance(this);
    activeController = uiController;
  }

  @Subscribe public void onLogout(LogoutEvent event) {
    if (uiController != null) {
      uiController.destroy(true);
      uiController = null;

      loginController = LoginController.newInstance(this);
      activeController = loginController;

      syncHandler.removeCallbacks(syncRunnable);

      CathodeApp.removeAccount(this);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Navigation callbacks
  ///////////////////////////////////////////////////////////////////////////

  @Override public void onDisplayShow(long showId, String title, LibraryType type) {
    activeController.onDisplayShow(showId, title, type);
  }

  @Override public void onDisplayEpisode(long episodeId, String showTitle) {
    activeController.onDisplayEpisode(episodeId, showTitle);
  }

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    activeController.onDisplaySeason(showId, seasonId, showTitle, seasonNumber, type);
  }

  @Override public void onStartShowSearch() {
    activeController.onStartShowSearch();
  }

  @Override public void onDisplayMovie(long movieId, String title) {
    activeController.onDisplayMovie(movieId, title);
  }

  @Override public void onStartMovieSearch() {
    activeController.onStartMovieSearch();
  }
}
