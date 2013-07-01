package net.simonvt.trakt.ui;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import net.simonvt.messagebar.MessageBar;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.event.AuthFailedEvent;
import net.simonvt.trakt.event.LoginEvent;
import net.simonvt.trakt.event.MessageEvent;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.sync.task.SyncTask;
import net.simonvt.trakt.ui.fragment.NavigationFragment;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.util.LogWrapper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import javax.inject.Inject;

public class HomeActivity extends BaseActivity
        implements NavigationFragment.OnMenuClickListener, ShowsNavigationListener, MoviesNavigationListener {

    private static final String TAG = "HomeActivity";

    private static final String STATE_LOGIN_CONTROLLER = "net.simonvt.trakt.ui.HomeActivity.loginController";
    private static final String STATE_UICONTROLLER = "net.simonvt.trakt.ui.HomeActivity.uiController";

    @Inject TraktTaskQueue mQueue;

    @Inject Bus mBus;

    protected MessageBar mMessageBar;

    private LoginController mLoginController;

    private UiController mUiController;

    private UiController mActiveController;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TraktApp.inject(this);

        mMessageBar = new MessageBar(this);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = settings.getString(Settings.USERNAME, null);
        final String password = settings.getString(Settings.PASSWORD, null);

        Bundle uiState = state != null ? state.getBundle(STATE_UICONTROLLER) : null;
        mUiController = PhoneController.newInstance(this);
        mUiController.onCreate(uiState);

        if (username == null || password == null) {
            Bundle loginState = state != null ? state.getBundle(STATE_LOGIN_CONTROLLER) : null;
            mLoginController = LoginController.newInstance(this);
            mLoginController.onCreate(loginState);

            mActiveController = mLoginController;

        } else {
            final long lastUpdated = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0);
            final long currentTimeSeconds = DateUtils.currentTimeSeconds();
            if (currentTimeSeconds >= lastUpdated + 6L * DateUtils.HOUR_IN_SECONDS) {
                LogWrapper.i(TAG, "Queueing SyncTask");
                settings.edit().putLong(Settings.SHOWS_LAST_UPDATED, currentTimeSeconds).apply();
                mQueue.add(new SyncTask());
            }

            mActiveController = mUiController;
        }

        mActiveController.onAttach();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLoginController != null) {
            outState.putBundle(STATE_LOGIN_CONTROLLER, mLoginController.onSaveInstanceState());
        }
        if (mUiController != null) {
            outState.putBundle(STATE_UICONTROLLER, mUiController.onSaveInstanceState());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBus.register(this);
    }

    @Override
    protected void onPause() {
        mBus.unregister(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (!mActiveController.onBackClicked()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mActiveController.onHomeClicked();
                return true;

        }

        return false;
    }

    @Override
    public void onMenuItemClicked(int id) {
        mActiveController.onMenuItemClicked(id);
    }

    @Subscribe
    public void onShowMessage(MessageEvent event) {
        mMessageBar.show(getString(event.getMessageRes()));
    }

    @Subscribe
    public void onAuthFailed(AuthFailedEvent event) {
        mUiController.onDetach();

        if (mLoginController == null) {
            mLoginController = LoginController.newInstance(this);
        }

        mActiveController = mLoginController;
        mActiveController.onAttach();
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        mLoginController.onDetach();
        mLoginController.onDestroy();
        mLoginController = null;

        mActiveController = mUiController;
        mActiveController.onAttach();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Navigation callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onDisplayShow(long showId, LibraryType type) {
        mActiveController.onDisplayShow(showId, type);
    }

    @Override
    public void onDisplaySeasons(long showId, LibraryType type) {
        mActiveController.onDisplaySeasons(showId, type);
    }

    @Override
    public void onDisplayEpisode(long episodeId, LibraryType type) {
        mActiveController.onDisplayEpisode(episodeId, type);
    }

    @Override
    public void onDisplaySeason(long showId, long seasonId, LibraryType type) {
        mActiveController.onDisplaySeason(showId, seasonId, type);
    }

    @Override
    public void onSearchShow(String query) {
        mActiveController.onSearchShow(query);
    }

    @Override
    public void onDisplayMovie(long movieId) {
        mActiveController.onDisplayMovie(movieId);
    }

    @Override
    public void onSearchMovie(String query) {
        mActiveController.onSearchMovie(query);
    }
}
