package net.simonvt.cathode.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.messagebar.MessageBar;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.LoginEvent;
import net.simonvt.cathode.event.MessageEvent;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.fragment.NavigationFragment;

public class HomeActivity extends BaseActivity
    implements NavigationFragment.OnMenuClickListener, ShowsNavigationListener,
    MoviesNavigationListener {

  private static final String TAG = "HomeActivity";

  private static final String STATE_LOGIN_CONTROLLER =
      "net.simonvt.cathode.ui.HomeActivity.loginController";
  private static final String STATE_UICONTROLLER = "net.simonvt.cathode.ui.HomeActivity.uiController";

  @Inject TraktTaskQueue queue;

  @Inject Bus bus;

  protected MessageBar messageBar;

  private LoginController loginController;

  private UiController uiController;

  private UiController activeController;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    CathodeApp.inject(this);

    setContentView(R.layout.ui_content_view);

    messageBar = new MessageBar(this);

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    final String username = settings.getString(Settings.USERNAME, null);
    final String password = settings.getString(Settings.PASSWORD, null);

    Bundle uiState = state != null ? state.getBundle(STATE_UICONTROLLER) : null;
    uiController = PhoneController.newInstance(this);
    uiController.onCreate(uiState);

    if (username == null || password == null) {
      Bundle loginState = state != null ? state.getBundle(STATE_LOGIN_CONTROLLER) : null;
      loginController = LoginController.newInstance(this);
      loginController.onCreate(loginState);

      activeController = loginController;
    } else {
      queue.add(new SyncTask());
      activeController = uiController;
    }

    activeController.onAttach();
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
    }

    return false;
  }

  @Override
  public void onMenuItemClicked(int id) {
    activeController.onMenuItemClicked(id);
  }

  @Subscribe
  public void onShowMessage(MessageEvent event) {
    messageBar.show(getString(event.getMessageRes()));
  }

  @Subscribe
  public void onAuthFailed(AuthFailedEvent event) {
    uiController.onDetach();

    if (loginController == null) {
      loginController = LoginController.newInstance(this);
    }

    activeController = loginController;
    activeController.onAttach();
  }

  @Subscribe
  public void onLogin(LoginEvent event) {
    loginController.onDetach();
    loginController.onDestroy();
    loginController = null;

    activeController = uiController;
    activeController.onAttach();
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
