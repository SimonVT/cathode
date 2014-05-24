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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MenuItem;
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
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.LoginEvent;
import net.simonvt.cathode.event.LogoutEvent;
import net.simonvt.cathode.event.MessageEvent;
import net.simonvt.cathode.event.SyncEvent;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncActivityStreamTask;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.dialog.LogoutDialog;
import net.simonvt.cathode.ui.fragment.NavigationFragment;
import net.simonvt.cathode.util.DateUtils;
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

  protected MessageBar messageBar;

  @InjectView(R.id.content) ViewGroup content;

  @InjectView(R.id.progress_top) ProgressBar progressTop;
  private ViewPropertyAnimator progressAnimator;

  private LoginController loginController;

  private UiController uiController;

  private UiController activeController;

  private long lastSync;

  private Handler handler;

  private Runnable displayLogin;

  private Runnable syncRunnable = new Runnable() {
    @Override public void run() {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this);
      final long lastFullSync = settings.getLong(Settings.FULL_SYNC, 0);
      final long currentTime = System.currentTimeMillis();
      if (lastFullSync + 24 * DateUtils.DAY_IN_MILLIS < currentTime) {
        queue.add(new SyncTask());
      } else {
        queue.add(new SyncActivityStreamTask());
      }
      lastSync = System.currentTimeMillis();
      handler.postDelayed(this, SYNC_DELAY);
    }
  };

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.activity_home);

    ButterKnife.inject(this);

    messageBar = new MessageBar(this);

    if (!CathodeApp.accountExists(this) || isLoginAction(getIntent())) {
      Bundle loginState = inState != null ? inState.getBundle(STATE_LOGIN_CONTROLLER) : null;
      loginController = LoginController.newInstance(this, content, loginState);

      activeController = loginController;
    } else {
      Bundle uiState = inState != null ? inState.getBundle(STATE_UICONTROLLER) : null;
      uiController = PhoneController.create(this, content, uiState);
      activeController = uiController;
    }

    handler = new Handler();

    if (inState != null) {
      lastSync = inState.getLong(STATE_LAST_SYNC);
    }
  }

  @Override protected void onNewIntent(Intent intent) {
    if (isLoginAction(intent)) {
      if (uiController != null) {
        displayLogin = new Runnable() {
          @Override public void run() {
            uiController.destroy(true);
            uiController = null;

            content.removeAllViews();

            loginController = LoginController.newInstance(HomeActivity.this, content);
            activeController = loginController;
            displayLogin = null;
          }
        };

        handler.post(displayLogin);
      }
    }
  }

  private boolean isLoginAction(Intent intent) {
    return ACTION_LOGIN.equals(intent.getAction());
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    if (loginController != null) {
      outState.putBundle(STATE_LOGIN_CONTROLLER, loginController.onSaveInstanceState());
    }
    if (uiController != null) {
      outState.putBundle(STATE_UICONTROLLER, uiController.onSaveInstanceState());
    }
    outState.putLong(STATE_LAST_SYNC, lastSync);
    super.onSaveInstanceState(outState);
  }

  @Override protected void onResume() {
    super.onResume();
    bus.register(this);

    if (uiController != null) {
      if (lastSync + SYNC_DELAY < System.currentTimeMillis()) {
        syncRunnable.run();
      } else {
        handler.postDelayed(syncRunnable, SYNC_DELAY);
      }
    }
  }

  @Override protected void onPause() {
    handler.removeCallbacks(syncRunnable);
    bus.unregister(this);
    super.onPause();
  }

  @Override protected void onDestroy() {
    if (displayLogin != null) {
      handler.removeCallbacks(displayLogin);
      displayLogin.run();
    }
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
    if (loginController != null) {
      loginController.destroy(true);
      loginController = null;
    }

    content.removeAllViews();

    uiController = PhoneController.create(this, content);
    activeController = uiController;
  }

  @Subscribe public void onLogout(LogoutEvent event) {
    if (uiController != null) {
      uiController.destroy(true);
      uiController = null;

      content.removeAllViews();

      loginController = LoginController.newInstance(this, content);
      activeController = loginController;

      handler.removeCallbacks(syncRunnable);

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
