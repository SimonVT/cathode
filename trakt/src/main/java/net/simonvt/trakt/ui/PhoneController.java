package net.simonvt.trakt.ui;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.trakt.R;
import net.simonvt.trakt.ui.fragment.BaseFragment;
import net.simonvt.trakt.ui.fragment.EpisodeFragment;
import net.simonvt.trakt.ui.fragment.EpisodesWatchlistFragment;
import net.simonvt.trakt.ui.fragment.MovieCollectionFragment;
import net.simonvt.trakt.ui.fragment.MovieFragment;
import net.simonvt.trakt.ui.fragment.MovieWatchlistFragment;
import net.simonvt.trakt.ui.fragment.NavigationFragment;
import net.simonvt.trakt.ui.fragment.SearchMovieFragment;
import net.simonvt.trakt.ui.fragment.SearchShowFragment;
import net.simonvt.trakt.ui.fragment.SeasonFragment;
import net.simonvt.trakt.ui.fragment.SeasonsFragment;
import net.simonvt.trakt.ui.fragment.ShowInfoFragment;
import net.simonvt.trakt.ui.fragment.ShowsCollectionFragment;
import net.simonvt.trakt.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.trakt.ui.fragment.UpcomingShowsFragment;
import net.simonvt.trakt.ui.fragment.WatchedMoviesFragment;
import net.simonvt.trakt.ui.fragment.WatchedShowsFragment;
import net.simonvt.trakt.util.FragmentStack;

import android.os.Bundle;

public class PhoneController extends UiController {

    private static final String TAG = "PhoneController";

    private static final String STATE_NAV_TITLE = "net.simonvt.trakt.ui.PhoneController.navTitle";

    private FragmentStack<BaseFragment> mStack;

    private String mNavTitle;

    private MenuDrawer mMenuDrawer;

    private NavigationFragment mNavigation;

    public static PhoneController newInstance(HomeActivity activity) {
        return new PhoneController(activity);
    }

    PhoneController(HomeActivity activity) {
        super(activity);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mMenuDrawer = MenuDrawer.attach(mActivity, MenuDrawer.Type.OVERLAY);
        mMenuDrawer.setSlideDrawable(R.drawable.ic_drawer);
        mMenuDrawer.setContentView(R.layout.activity_home);
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_NONE);

        mNavigation = (NavigationFragment) mActivity.getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_NAVIGATION);

        if (mNavigation == null) {
            mNavigation = new NavigationFragment();
            mActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(mMenuDrawer.getMenuContainer().getId(), mNavigation, FRAGMENT_NAVIGATION)
                    .commit();
        }

        mStack = FragmentStack.forContainer(mActivity.getSupportFragmentManager(), R.id.content,
                new FragmentStack.Callback<BaseFragment>() {
                    @Override
                    public void onStackChanged(int stackSize, BaseFragment topFragment) {
                        mMenuDrawer.setDrawerIndicatorEnabled(stackSize <= 1);
                        if (stackSize > 1) {
                            mActivity.getActionBar().setTitle(R.string.app_name);
                        } else {
                            mActivity.getActionBar().setTitle(mNavTitle);
                        }
                    }
                });

        mMenuDrawer.setOnDrawerStateChangeListener(new MenuDrawer.OnDrawerStateChangeListener() {
            @Override
            public void onDrawerStateChange(int oldState, int newState) {
                switch (newState) {
                    case MenuDrawer.STATE_CLOSED:
                        if (!mStack.commit()) {
                            if (mStack.getStackSize() > 1) {
                                mActivity.getActionBar().setTitle(R.string.app_name);
                            } else {
                                mActivity.getActionBar().setTitle(mNavTitle);
                            }
                        }

                        mStack.getTopFragment().setMenuVisibility(true);
                        break;

                    default:
                        mStack.getTopFragment().setMenuVisibility(false);
                        mStack.getTopFragment().setMenuVisibility(false);
                        mActivity.getActionBar().setTitle(R.string.app_name);
                }
            }

            @Override
            public void onDrawerSlide(float openRatio, int offsetPixels) {
            }
        });

        if (state != null) {
            mStack.onRestoreInstanceState(state);
        }
    }

    @Override
    public boolean onBackClicked() {
        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
            return true;
        }

        if (mStack.popStack(true)) {
            return true;
        }

        return false;
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();
        mStack.onSaveInstanceState(state);
        return state;
    }

    @Override
    public void onAttach() {
        super.onAttach();
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_BEZEL);
        mMenuDrawer.setDrawerIndicatorEnabled(mStack.getStackSize() == 1);

        mActivity.getActionBar().setHomeButtonEnabled(true);
        mActivity.getActionBar().setDisplayHomeAsUpEnabled(true);

        if (mStack.getStackSize() == 0) {
            UpcomingShowsFragment f = new UpcomingShowsFragment();
            mStack.setTopFragment(f, FRAGMENT_SHOWS_UPCOMING);
            mStack.commit();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onHomeClicked() {
        super.onHomeClicked();
        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
        } else if (!mStack.popStack(drawerState == MenuDrawer.STATE_CLOSED)) {
            mMenuDrawer.toggleMenu();
        }
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
        MovieFragment fragment = MovieFragment.newInstance(movieId);
        mStack.addFragment(fragment, FRAGMENT_MOVIE);
        mStack.commit();
    }

    @Override
    public void onSearchMovie(String query) {
        SearchMovieFragment fragment = SearchMovieFragment.newInstance(query);
        mStack.addFragment(fragment, FRAGMENT_SEARCH_MOVIE);
        mStack.commit();
    }
}
