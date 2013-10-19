package net.simonvt.cathode.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.LoginEvent;
import net.simonvt.cathode.event.LogoutEvent;
import net.simonvt.cathode.event.MessageEvent;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.ui.dialog.LogoutDialog;
import net.simonvt.cathode.ui.fragment.NavigationFragment;
import net.simonvt.messagebar.MessageBar;

public class HomeActivity extends BaseActivity
    implements NavigationFragment.OnMenuClickListener, ShowsNavigationListener,
    MoviesNavigationListener {

  private static final String TAG = "HomeActivity";

  private static final String STATE_LOGIN_CONTROLLER =
      "net.simonvt.cathode.ui.HomeActivity.loginController";
  private static final String STATE_UICONTROLLER =
      "net.simonvt.cathode.ui.HomeActivity.uiController";

  public static final String ACTION_LOGIN = "net.simonvt.cathode.intent.action.LOGIN";
  public static final String DIALOG_LOGOUT = "net.simonvt.cathode.ui.HomeActivity.logoutDialog";

  @Inject TraktTaskQueue queue;

  @Inject Bus bus;

  protected MessageBar messageBar;

  private LoginController loginController;

  private UiController uiController;

  private UiController activeController;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.ui_content_view);

    messageBar = new MessageBar(this);

    if (!CathodeApp.accountExists(this) || isLoginAction(getIntent())) {
      Bundle loginState = state != null ? state.getBundle(STATE_LOGIN_CONTROLLER) : null;
      loginController = LoginController.newInstance(this);
      loginController.onCreate(loginState);

      activeController = loginController;
    } else {
      queue.add(new SyncTask());

      Bundle uiState = state != null ? state.getBundle(STATE_UICONTROLLER) : null;
      uiController = PhoneController.newInstance(this);
      uiController.onCreate(uiState);
      activeController = uiController;
    }

    activeController.onAttach();
  }

  @Override protected void onNewIntent(Intent intent) {
    if (isLoginAction(intent)) {
      if (uiController != null) {
        uiController.onDestroy(true);
        uiController = null;

        loginController = LoginController.newInstance(this);
        loginController.onCreate(null);
        loginController.onAttach();
        activeController = loginController;
      }
    }
  }

  private boolean isLoginAction(Intent intent) {
    return ACTION_LOGIN.equals(intent.getAction());
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (loginController != null) {
      outState.putBundle(STATE_LOGIN_CONTROLLER, loginController.onSaveInstanceState());
    }
    if (uiController != null) {
      outState.putBundle(STATE_UICONTROLLER, uiController.onSaveInstanceState());
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    bus.register(this);
  }

  @Override
  protected void onPause() {
    bus.unregister(this);
    super.onPause();
  }

  @Override
  public void onBackPressed() {
    if (!activeController.onBackClicked()) {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
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

  @Override
  public void onMenuItemClicked(int id) {
    activeController.onMenuItemClicked(id);
  }

  @Subscribe public void onShowMessage(MessageEvent event) {
    messageBar.show(getString(event.getMessageRes()));
  }

  @Subscribe public void onAuthFailed(AuthFailedEvent event) {
  }

  @Subscribe public void onLogin(LoginEvent event) {
    loginController.onDestroy(true);
    loginController = null;

    uiController = PhoneController.newInstance(this);
    uiController.onCreate(null);
    uiController.onAttach();
    activeController = uiController;
  }

  @Subscribe public void onLogout(LogoutEvent event) {
    if (uiController != null) {
      uiController.onDestroy(true);
      uiController = null;

      loginController = LoginController.newInstance(this);
      loginController.onCreate(null);
      loginController.onAttach();
      activeController = loginController;

      CathodeApp.removeAccount(this);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Navigation callbacks
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public void onDisplayShow(long showId, String title, LibraryType type) {
    activeController.onDisplayShow(showId, title, type);
  }

  @Override
  public void onDisplayEpisode(long episodeId, String showTitle) {
    activeController.onDisplayEpisode(episodeId, showTitle);
  }

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    activeController.onDisplaySeason(showId, seasonId, showTitle, seasonNumber, type);
  }

  @Override
  public void onStartShowSearch() {
    activeController.onStartShowSearch();
  }

  @Override
  public void onDisplayMovie(long movieId, String title) {
    activeController.onDisplayMovie(movieId, title);
  }

  @Override
  public void onStartMovieSearch() {
    activeController.onStartMovieSearch();
  }
}
