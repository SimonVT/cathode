package net.simonvt.trakt.ui;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.messagebar.MessageBar;
import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.event.AuthFailedEvent;
import net.simonvt.trakt.event.LoginEvent;
import net.simonvt.trakt.event.MessageEvent;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.sync.task.SyncTask;
import net.simonvt.trakt.ui.fragment.SearchShowFragment;
import net.simonvt.trakt.ui.fragment.BaseFragment;
import net.simonvt.trakt.ui.fragment.EpisodeFragment;
import net.simonvt.trakt.ui.fragment.EpisodesWatchlistFragment;
import net.simonvt.trakt.ui.fragment.LoginFragment;
import net.simonvt.trakt.ui.fragment.MovieCollectionFragment;
import net.simonvt.trakt.ui.fragment.MovieWatchlistFragment;
import net.simonvt.trakt.ui.fragment.NavigationFragment;
import net.simonvt.trakt.ui.fragment.SearchMovieFragment;
import net.simonvt.trakt.ui.fragment.SeasonFragment;
import net.simonvt.trakt.ui.fragment.SeasonsFragment;
import net.simonvt.trakt.ui.fragment.ShowInfoFragment;
import net.simonvt.trakt.ui.fragment.ShowsCollectionFragment;
import net.simonvt.trakt.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.trakt.ui.fragment.UpcomingShowsFragment;
import net.simonvt.trakt.ui.fragment.WatchedMoviesFragment;
import net.simonvt.trakt.ui.fragment.WatchedShowsFragment;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.util.FragmentStack;
import net.simonvt.trakt.util.LogWrapper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

public class HomeActivity extends BaseActivity
        implements NavigationFragment.OnMenuClickListener, ShowsNavigationListener, MoviesNavigationListener {

    private static final String TAG = "HomeActivity";

    private static final String STATE_NAV_TITLE = "net.simonvt.trakt.ui.HomeActivity.navTitle";

    private static final String FRAGMENT_LOGIN = "net.simonvt.trakt.ui.HomeActivity.loginFragment";
    private static final String FRAGMENT_NAVIGATION = "net.simonvt.trakt.ui.HomeActivity.navigationFragment";
    private static final String FRAGMENT_SHOWS = "net.simonvt.trakt.ui.HomeActivity.showsFragment";
    private static final String FRAGMENT_SHOWS_UPCOMING = "net.simonvt.trakt.ui.HomeActivity.upcomingShowsFragment";
    private static final String FRAGMENT_SHOWS_COLLECTION =
            "net.simonvt.trakt.ui.HomeActivity.collectionShowsFragment";
    private static final String FRAGMENT_SHOW = "net.simonvt.trakt.ui.HomeActivity.showFragment";
    private static final String FRAGMENT_SEASONS = "net.simonvt.trakt.ui.HomeActivity.seasonsFragment";
    private static final String FRAGMENT_SEASON = "net.simonvt.trakt.ui.HomeActivity.seasonFragment";
    private static final String FRAGMENT_EPISODE = "net.simonvt.trakt.ui.HomeActivity.episodeFragment";
    private static final String FRAGMENT_SHOWS_WATCHLIST = "net.simonvt.trakt.ui.HomeActivity.showsWatchlistFragment";
    private static final String FRAGMENT_EPISODES_WATCHLIST =
            "net.simonvt.trakt.ui.HomeActivity.episodesWatchlistFragment";
    private static final String FRAGMENT_ADD_SHOW = "net.simonvt.trakt.ui.HomeActivity.addShowFragment";
    private static final String FRAGMENT_MOVIES_WATCHED = "net.simonvt.trakt.ui.HomeActivity.moviesWatched";
    private static final String FRAGMENT_MOVIES_COLLECTION = "net.simonvt.trakt.ui.HomeActivity.moviesCollection";
    private static final String FRAGMENT_MOVIES_WATCHLIST = "net.simonvt.trakt.ui.HomeActivity.moviesWatchlist";
    private static final String FRAGMENT_SEARCH_MOVIE = "net.simonvt.trakt.ui.HomeActivity.searchMovieFragment";

    @Inject TraktTaskQueue mQueue;

    @Inject Bus mBus;

    private MenuDrawer mMenuDrawer;

    private NavigationFragment mNavigation;

    protected MessageBar mMessageBar;

    private FragmentStack<BaseFragment> mStack;

    private String mNavTitle = "Watched shows";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TraktApp.inject(this);

        mNavigation = findFragment(FRAGMENT_NAVIGATION);

        mMenuDrawer = MenuDrawer.attach(this);
        mMenuDrawer.setSlideDrawable(R.drawable.ic_drawer);
        mMenuDrawer.setContentView(R.layout.activity_home);
        mMenuDrawer.setOnDrawerStateChangeListener(new MenuDrawer.OnDrawerStateChangeListener() {
            @Override
            public void onDrawerStateChange(int oldState, int newState) {
                switch (newState) {
                    case MenuDrawer.STATE_CLOSED:
                        if (!mStack.commit()) {
                            if (mStack.getStackSize() > 1) {
                                getActionBar().setTitle(R.string.app_name);
                            } else {
                                getActionBar().setTitle(mNavTitle);
                            }
                        }

                        mStack.getTopFragment().setMenuVisibility(true);
                        break;

                    default:
                        mStack.getTopFragment().setMenuVisibility(false);
                        mStack.getTopFragment().setMenuVisibility(false);
                        getActionBar().setTitle(R.string.app_name);
                }
            }

            @Override
            public void onDrawerSlide(float openRatio, int offsetPixels) {
            }
        });
        mMessageBar = new MessageBar(this);

        mStack = FragmentStack.forContainer(getSupportFragmentManager(), R.id.content,
                new FragmentStack.Callback<BaseFragment>() {
                    @Override
                    public void onStackChanged(int stackSize, BaseFragment topFragment) {
                        mMenuDrawer.setDrawerIndicatorEnabled(
                                stackSize <= 1 && !FRAGMENT_LOGIN.equals(topFragment.getTag()));
                        getActionBar().setDisplayHomeAsUpEnabled(!FRAGMENT_LOGIN.equals(topFragment.getTag()));
                        if (stackSize > 1) {
                            getActionBar().setTitle(R.string.app_name);
                        } else {
                            getActionBar().setTitle(mNavTitle);
                        }
                    }
                });

        if (mNavigation == null) {
            mNavigation = new NavigationFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(mMenuDrawer.getMenuContainer().getId(), mNavigation, FRAGMENT_NAVIGATION)
                    .commit();
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = settings.getString(Settings.USERNAME, null);
        final String password = settings.getString(Settings.PASSWORD, null);

        if (username == null || password == null) {
            mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_NONE);
            if (state == null) {
                LoginFragment loginFragment = new LoginFragment();
                mStack.setTopFragment(loginFragment, FRAGMENT_LOGIN);
                mStack.commit();
            }

        } else {
            getActionBar().setDisplayHomeAsUpEnabled(true);

            final long lastUpdated = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0);
            final long currentTimeSeconds = DateUtils.currentTimeSeconds();
            if (currentTimeSeconds >= lastUpdated + 6L * DateUtils.HOUR_IN_SECONDS) {
                LogWrapper.i(TAG, "Queueing SyncTask");
                settings.edit().putLong(Settings.SHOWS_LAST_UPDATED, currentTimeSeconds).apply();
                mQueue.add(new SyncTask());
            }

            if (state == null) {
                WatchedShowsFragment watchedShowsFragment = new WatchedShowsFragment();
                mStack.setTopFragment(watchedShowsFragment, FRAGMENT_SHOWS);

                mStack.commit();
                getActionBar().setTitle(mNavTitle);
            } else {
                mNavTitle = state.getString(STATE_NAV_TITLE);
                mStack.onRestoreInstanceState(state);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mStack.onSaveInstanceState(outState);
        outState.putString(STATE_NAV_TITLE, mNavTitle);
        super.onSaveInstanceState(outState);
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
        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
            return;
        }

        if (mStack.popStack(true)) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                final int drawerState = mMenuDrawer.getDrawerState();
                if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
                    mMenuDrawer.closeMenu();
                } else if (!mStack.popStack(drawerState == MenuDrawer.STATE_CLOSED)) {
                    mMenuDrawer.toggleMenu();
                }

                return true;

        }

        return false;
    }

    @Override
    public void onMenuItemClicked(int id) {
        switch (id) {
            case R.id.menu_shows_upcoming:
                UpcomingShowsFragment upcomingShowsFragment = new UpcomingShowsFragment();
                mStack.setTopFragment(upcomingShowsFragment, FRAGMENT_SHOWS_UPCOMING);
                mNavTitle = "Upcoming shows";
                break;

            case R.id.menu_shows_watched:
                WatchedShowsFragment watchedShowsFragment = new WatchedShowsFragment();
                mStack.setTopFragment(watchedShowsFragment, FRAGMENT_SHOWS);
                mNavTitle = "Watched shows";
                break;

            case R.id.menu_shows_collection:
                ShowsCollectionFragment showsCollectionFragment = new ShowsCollectionFragment();
                mStack.setTopFragment(showsCollectionFragment, FRAGMENT_SHOWS_COLLECTION);
                mNavTitle = "Shows collection";
                break;

            case R.id.menu_shows_watchlist:
                ShowsWatchlistFragment showsWatchlistFragment = new ShowsWatchlistFragment();
                mStack.setTopFragment(showsWatchlistFragment, FRAGMENT_SHOWS_WATCHLIST);
                mNavTitle = "Shows watchlist";
                break;

            case R.id.menu_episodes_watchlist:
                EpisodesWatchlistFragment episodesWatchlistFragment = new EpisodesWatchlistFragment();
                mStack.setTopFragment(episodesWatchlistFragment, FRAGMENT_EPISODES_WATCHLIST);
                mNavTitle = "Episodes watchlist";
                break;

            // case R.id.menu_shows_ratings:
            // case R.id.menu_shows_charts:

            case R.id.menu_movies_watched:
                WatchedMoviesFragment watchedMoviesFragment = new WatchedMoviesFragment();
                mStack.setTopFragment(watchedMoviesFragment, FRAGMENT_MOVIES_WATCHED);
                mNavTitle = "Watched movies";
                break;

            case R.id.menu_movies_collection:
                MovieCollectionFragment movieCollectionFragment = new MovieCollectionFragment();
                mStack.setTopFragment(movieCollectionFragment, FRAGMENT_MOVIES_COLLECTION);
                mNavTitle = "Movie collection";
                break;

            case R.id.menu_movies_watchlist:
                MovieWatchlistFragment movieWatchlistFragment = new MovieWatchlistFragment();
                mStack.setTopFragment(movieWatchlistFragment, FRAGMENT_MOVIES_WATCHLIST);
                mNavTitle = "Movie watchlist";
                break;

            // case R.id.menu_movies_ratings:
            // case R.id.menu_movies_charts:
        }

        mMenuDrawer.closeMenu();
    }

    @Override
    public void onActiveViewChanged(int position, View activeView) {
        mMenuDrawer.setActiveView(activeView, position);
    }

    @Subscribe
    public void onShowMessage(MessageEvent event) {
        mMessageBar.show(getString(event.getMessageRes()));
    }

    @Subscribe
    public void onAuthFailed(AuthFailedEvent event) {
        // TODO: Handle it
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_BEZEL);

        WatchedShowsFragment watchedShowsFragment = new WatchedShowsFragment();

        mStack.setTopFragment(watchedShowsFragment, FRAGMENT_SHOWS);
        mStack.commit();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Navigation callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onDisplayShow(long showId, LibraryType type) {
        ShowInfoFragment showInfoFragment = ShowInfoFragment.newInstance(showId, type);
        mStack.addFragment(showInfoFragment, FRAGMENT_SHOW);
        mStack.commit();
    }

    @Override
    public void onDisplaySeasons(long showId, LibraryType type) {
        SeasonsFragment sasonsFragment = SeasonsFragment.newInstance(showId, type);
        mStack.addFragment(sasonsFragment, FRAGMENT_SEASONS);
        mStack.commit();
    }

    @Override
    public void onDisplayEpisode(long episodeId, LibraryType type) {
        EpisodeFragment episodeFragment = EpisodeFragment.newInstance(episodeId);
        mStack.addFragment(episodeFragment, FRAGMENT_EPISODE);
        mStack.commit();
    }

    @Override
    public void onDisplaySeason(long showId, long seasonId, LibraryType type) {
        SeasonFragment seasonFragment = SeasonFragment.newInstance(showId, seasonId, type);
        mStack.addFragment(seasonFragment, FRAGMENT_SEASON);
        mStack.commit();
    }

    @Override
    public void onSearchShow(String query) {
        SearchShowFragment fragment = SearchShowFragment.newInstance(query);
        mStack.addFragment(fragment, FRAGMENT_ADD_SHOW);
        mStack.commit();
    }

    @Override
    public void onDisplayMovie(long movieId) {
    }

    @Override
    public void onSearchMovie(String query) {
        SearchMovieFragment fragment = SearchMovieFragment.newInstance(query);
        mStack.addFragment(fragment, FRAGMENT_SEARCH_MOVIE);
        mStack.commit();
    }
}
