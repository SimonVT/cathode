package net.simonvt.trakt.ui;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.event.OnTitleChangedEvent;
import net.simonvt.trakt.ui.fragment.EpisodeFragment;
import net.simonvt.trakt.ui.fragment.EpisodesWatchlistFragment;
import net.simonvt.trakt.ui.fragment.MovieCollectionFragment;
import net.simonvt.trakt.ui.fragment.MovieFragment;
import net.simonvt.trakt.ui.fragment.MovieWatchlistFragment;
import net.simonvt.trakt.ui.fragment.NavigationFragment;
import net.simonvt.trakt.ui.fragment.SearchMovieFragment;
import net.simonvt.trakt.ui.fragment.SearchShowFragment;
import net.simonvt.trakt.ui.fragment.SeasonFragment;
import net.simonvt.trakt.ui.fragment.ShowInfoFragment;
import net.simonvt.trakt.ui.fragment.ShowsCollectionFragment;
import net.simonvt.trakt.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.trakt.ui.fragment.UpcomingShowsFragment;
import net.simonvt.trakt.ui.fragment.WatchedMoviesFragment;
import net.simonvt.trakt.ui.fragment.WatchedShowsFragment;
import net.simonvt.trakt.util.FragmentStack;
import net.simonvt.trakt.util.LogWrapper;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.SearchView;

import javax.inject.Inject;

public class PhoneController extends UiController {

    private static final String TAG = "PhoneController";

    private static final String STATE_SEARCH_TYPE = "net.simonvt.trakt.ui.PhoneController.searchType";
    private static final String STATE_SEARCH_QUERY = "net.simonvt.trakt.ui.PhoneController.searchQuery";

    private static final int SEARCH_TYPE_SHOW = 1;
    private static final int SEARCH_TYPE_MOVIE = 2;

    @Inject Bus mBus;

    private FragmentStack mStack;

    private MenuDrawer mMenuDrawer;

    private NavigationFragment mNavigation;

    private int mSearchType;
    private SearchView mSearchView;

    private boolean mIsTablet;

    public static PhoneController newInstance(HomeActivity activity) {
        return new PhoneController(activity);
    }

    PhoneController(HomeActivity activity) {
        super(activity);
        TraktApp.inject(activity, this);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mIsTablet = mActivity.getResources().getBoolean(R.bool.isTablet);

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

        mStack = FragmentStack.forContainer(mActivity, R.id.content,
                new FragmentStack.Callback() {
                    @Override
                    public void onStackChanged(int stackSize, Fragment topFragment) {
                        LogWrapper.v(TAG, "[onStackChanged] " + topFragment.getTag());
                        FragmentContract fragment = (FragmentContract) topFragment;

                        mMenuDrawer.setDrawerIndicatorEnabled(stackSize <= 1);
                        if (!mMenuDrawer.isMenuVisible()) {
                            String title = fragment.getTitle();
                            if (title != null) {
                                mActivity.getActionBar().setTitle(title);
                            } else {
                                mActivity.getActionBar().setTitle(R.string.app_name);
                            }
                            mActivity.getActionBar().setSubtitle(fragment.getSubtitle());
                        }
                        if (mSearchView != null) {
                            if (!FRAGMENT_SEARCH_MOVIE.equals(topFragment.getTag())
                                    && !FRAGMENT_SEARCH_SHOW.equals(topFragment.getTag())) {
                                mActivity.getActionBar().setDisplayShowCustomEnabled(false);
                                mActivity.getActionBar().setCustomView(null);
                                mSearchView = null;
                            }
                        }
                        topFragment.setMenuVisibility(mSearchView == null);
                    }
                });

        mMenuDrawer.setOnDrawerStateChangeListener(new MenuDrawer.OnDrawerStateChangeListener() {
            @Override
            public void onDrawerStateChange(int oldState, int newState) {
                switch (newState) {
                    case MenuDrawer.STATE_CLOSED:
                        if (!mStack.commit()) {
                            String title = ((FragmentContract) mStack.getTopFragment()).getTitle();
                            if (title != null) {
                                mActivity.getActionBar().setTitle(title);
                            } else {
                                mActivity.getActionBar().setTitle(R.string.app_name);
                            }
                            mActivity.getActionBar()
                                    .setSubtitle(((FragmentContract) mStack.getTopFragment()).getSubtitle());
                        }

                        if (mSearchView != null) {
                            mActivity.getActionBar().setDisplayShowCustomEnabled(true);
                        } else {
                            mStack.getTopFragment().setMenuVisibility(true);
                        }
                        break;

                    default:
                        mStack.getTopFragment().setMenuVisibility(false);
                        mActivity.getActionBar().setDisplayShowCustomEnabled(false);
                        mActivity.getActionBar().setTitle(R.string.app_name);
                        break;
                }
            }

            @Override
            public void onDrawerSlide(float openRatio, int offsetPixels) {
            }
        });

        if (state != null) {
            mStack.onRestoreInstanceState(state);
            CharSequence query = state.getCharSequence(STATE_SEARCH_QUERY);
            if (query != null) {
                mSearchType = state.getInt(STATE_SEARCH_TYPE);
                createSearchView(mSearchType);
                mSearchView.setQuery(query, false);
            }
        }
    }

    @Subscribe
    public void onTitleChanged(OnTitleChangedEvent event) {
        if (!mMenuDrawer.isMenuVisible()) {
            String title = ((FragmentContract) mStack.getTopFragment()).getTitle();
            if (title != null) {
                mActivity.getActionBar().setTitle(title);
            } else {
                mActivity.getActionBar().setTitle(R.string.app_name);
            }
            mActivity.getActionBar().setSubtitle(((FragmentContract) mStack.getTopFragment()).getSubtitle());
        }
    }

    @Override
    public boolean onBackClicked() {
        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
            return true;
        }

        if (mSearchView != null) {
            mActivity.getActionBar().setDisplayShowCustomEnabled(false);
            mActivity.getActionBar().setCustomView(null);
            mStack.getTopFragment().setMenuVisibility(true);
            mSearchView = null;
            return true;
        }

        final FragmentContract topFragment = (FragmentContract) mStack.getTopFragment();
        if (topFragment != null && topFragment.onBackPressed()) {
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
        if (mSearchView != null) {
            state.putInt(STATE_SEARCH_TYPE, mSearchType);
            state.putCharSequence(STATE_SEARCH_QUERY, mSearchView.getQuery());
        }
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
            mStack.setTopFragment(UpcomingShowsFragment.class, FRAGMENT_SHOWS_UPCOMING);
            mStack.commit();
        }

        mBus.register(this);
    }

    @Override
    public void onDetach() {
        mBus.unregister(this);
        super.onDetach();
    }

    @Override
    public void onHomeClicked() {
        super.onHomeClicked();

        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
        } else if (mSearchView != null) {
            mActivity.getActionBar().setDisplayShowCustomEnabled(false);
            mActivity.getActionBar().setCustomView(null);
            mStack.getTopFragment().setMenuVisibility(true);
            mSearchView = null;
        } else if (!mStack.popStack(drawerState == MenuDrawer.STATE_CLOSED)) {
            mMenuDrawer.toggleMenu();
        }
    }

    @Override
    public void onMenuItemClicked(int id) {
        switch (id) {
            case R.id.menu_shows_upcoming:
                mStack.setTopFragment(UpcomingShowsFragment.class, FRAGMENT_SHOWS_UPCOMING);
                break;

            case R.id.menu_shows_watched:
                mStack.setTopFragment(WatchedShowsFragment.class, FRAGMENT_SHOWS);
                break;

            case R.id.menu_shows_collection:
                mStack.setTopFragment(ShowsCollectionFragment.class, FRAGMENT_SHOWS_COLLECTION);
                break;

            case R.id.menu_shows_watchlist:
                mStack.setTopFragment(ShowsWatchlistFragment.class, FRAGMENT_SHOWS_WATCHLIST);
                break;

            case R.id.menu_episodes_watchlist:
                mStack.setTopFragment(EpisodesWatchlistFragment.class, FRAGMENT_EPISODES_WATCHLIST);
                break;

            // case R.id.menu_shows_ratings:
            // case R.id.menu_shows_charts:

            case R.id.menu_movies_watched:
                mStack.setTopFragment(WatchedMoviesFragment.class, FRAGMENT_MOVIES_WATCHED);
                break;

            case R.id.menu_movies_collection:
                mStack.setTopFragment(MovieCollectionFragment.class, FRAGMENT_MOVIES_COLLECTION);
                break;

            case R.id.menu_movies_watchlist:
                mStack.setTopFragment(MovieWatchlistFragment.class, FRAGMENT_MOVIES_WATCHLIST);
                break;

            // case R.id.menu_movies_ratings:
            // case R.id.menu_movies_charts:
        }

        mMenuDrawer.closeMenu();
    }

    private void createSearchView(int searchType) {
        mSearchType = searchType;
        mSearchView = (SearchView) LayoutInflater.from(mActivity.getActionBar().getThemedContext())
                .inflate(R.layout.search, null);
        mSearchView.onActionViewExpanded();
        mActivity.getActionBar().setDisplayShowCustomEnabled(true);
        mActivity.getActionBar().setCustomView(mSearchView,
                new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        mStack.getTopFragment().setMenuVisibility(false);
        mMenuDrawer.setDrawerIndicatorEnabled(false);
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mSearchView = null;
                mStack.getTopFragment().setMenuVisibility(true);
                return true;
            }
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                LogWrapper.v(TAG, "[onQueryTextSubmit] Query: " + query);
                if (mSearchType == SEARCH_TYPE_MOVIE) {
                    SearchMovieFragment f = mStack.getFragment(FRAGMENT_SEARCH_MOVIE);
                    if (f == null) {
                        mStack.addFragment(SearchMovieFragment.class, FRAGMENT_SEARCH_MOVIE,
                                SearchMovieFragment.getArgs(query));
                        mStack.commit();
                    } else {
                        f.query(query);
                    }
                } else {
                    SearchShowFragment f = mStack.getFragment(FRAGMENT_SEARCH_SHOW);
                    if (f == null) {
                        mStack.addFragment(SearchShowFragment.class, FRAGMENT_SEARCH_SHOW,
                                SearchShowFragment.getArgs(query));
                        mStack.commit();
                    } else {
                        f.query(query);
                    }
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Navigation callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onDisplayShow(long showId, String title, LibraryType type) {
        mStack.addFragment(ShowInfoFragment.class, FRAGMENT_SHOW, ShowInfoFragment.getArgs(showId, title, type));
        mStack.commit();
    }

    @Override
    public void onDisplayEpisode(long episodeId, String showTitle) {
        Bundle args = EpisodeFragment.getArgs(episodeId, showTitle);
        if (mIsTablet) {
            EpisodeFragment f =
                    (EpisodeFragment) Fragment.instantiate(mActivity, EpisodeFragment.class.getName(), args);
            f.show(mActivity.getSupportFragmentManager(), FRAGMENT_EPISODE);
        } else {
            mStack.addFragment(EpisodeFragment.class, FRAGMENT_EPISODE, EpisodeFragment.getArgs(episodeId, showTitle));
            mStack.commit();
        }
    }

    @Override
    public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber, LibraryType type) {
        mStack.addFragment(SeasonFragment.class, FRAGMENT_SEASON,
                SeasonFragment.getArgs(showId, seasonId, showTitle, seasonNumber, type));
        mStack.commit();
    }

    @Override
    public void onStartShowSearch() {
        super.onStartShowSearch();
        createSearchView(SEARCH_TYPE_SHOW);
    }

    @Override
    public void onDisplayMovie(long movieId, String title) {
        mStack.addFragment(MovieFragment.class, FRAGMENT_MOVIE, MovieFragment.getArgs(movieId, title));
        mStack.commit();
    }

    @Override
    public void onStartMovieSearch() {
        super.onStartMovieSearch();
        createSearchView(SEARCH_TYPE_MOVIE);
    }
}
