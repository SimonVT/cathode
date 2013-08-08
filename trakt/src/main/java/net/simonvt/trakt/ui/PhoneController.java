package net.simonvt.trakt.ui;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
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

public class PhoneController extends UiController {

  private static final String TAG = "PhoneController";

  private static final String STATE_SEARCH_TYPE = "net.simonvt.trakt.ui.PhoneController.searchType";
  private static final String STATE_SEARCH_QUERY =
      "net.simonvt.trakt.ui.PhoneController.searchQuery";

  private static final int SEARCH_TYPE_SHOW = 1;
  private static final int SEARCH_TYPE_MOVIE = 2;

  @Inject Bus bus;

  private FragmentStack stack;

  private MenuDrawer menuDrawer;

  private NavigationFragment navigation;

  private int searchType;
  private SearchView searchView;

  private boolean isTablet;

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
    isTablet = activity.getResources().getBoolean(R.bool.isTablet);

    menuDrawer = MenuDrawer.attach(activity, MenuDrawer.Type.OVERLAY);
    menuDrawer.setSlideDrawable(R.drawable.ic_drawer);
    menuDrawer.setContentView(R.layout.activity_home);
    menuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_NONE);

    navigation = (NavigationFragment) activity.getSupportFragmentManager()
        .findFragmentByTag(FRAGMENT_NAVIGATION);

    if (navigation == null) {
      navigation = new NavigationFragment();
      activity.getSupportFragmentManager()
          .beginTransaction()
          .add(menuDrawer.getMenuContainer().getId(), navigation, FRAGMENT_NAVIGATION)
          .commit();
    }

    stack = FragmentStack.forContainer(activity, R.id.content, new FragmentStack.Callback() {
      @Override
      public void onStackChanged(int stackSize, Fragment topFragment) {
        LogWrapper.v(TAG, "[onStackChanged] " + topFragment.getTag());
        FragmentContract fragment = (FragmentContract) topFragment;

        menuDrawer.setDrawerIndicatorEnabled(stackSize <= 1);
        if (!menuDrawer.isMenuVisible()) {
          String title = fragment.getTitle();
          if (title != null) {
            activity.getActionBar().setTitle(title);
          } else {
            activity.getActionBar().setTitle(R.string.app_name);
          }
          activity.getActionBar().setSubtitle(fragment.getSubtitle());
        }
        if (searchView != null) {
          if (!FRAGMENT_SEARCH_MOVIE.equals(topFragment.getTag()) && !FRAGMENT_SEARCH_SHOW.equals(
              topFragment.getTag())) {
            activity.getActionBar().setDisplayShowCustomEnabled(false);
            activity.getActionBar().setCustomView(null);
            searchView = null;
          }
        }
        topFragment.setMenuVisibility(searchView == null);
      }
    });

    menuDrawer.setOnDrawerStateChangeListener(new MenuDrawer.OnDrawerStateChangeListener() {
      @Override
      public void onDrawerStateChange(int oldState, int newState) {
        switch (newState) {
          case MenuDrawer.STATE_CLOSED:
            if (!stack.commit()) {
              String title = ((FragmentContract) stack.getTopFragment()).getTitle();
              if (title != null) {
                activity.getActionBar().setTitle(title);
              } else {
                activity.getActionBar().setTitle(R.string.app_name);
              }
              activity.getActionBar()
                  .setSubtitle(((FragmentContract) stack.getTopFragment()).getSubtitle());
            }

            if (searchView != null) {
              activity.getActionBar().setDisplayShowCustomEnabled(true);
            } else {
              stack.getTopFragment().setMenuVisibility(true);
            }
            break;

          default:
            stack.getTopFragment().setMenuVisibility(false);
            activity.getActionBar().setDisplayShowCustomEnabled(false);
            activity.getActionBar().setTitle(R.string.app_name);
            break;
        }
      }

      @Override
      public void onDrawerSlide(float openRatio, int offsetPixels) {
      }
    });

    if (state != null) {
      stack.onRestoreInstanceState(state);
      CharSequence query = state.getCharSequence(STATE_SEARCH_QUERY);
      if (query != null) {
        searchType = state.getInt(STATE_SEARCH_TYPE);
        createSearchView(searchType);
        searchView.setQuery(query, false);
      }
    }
  }

  @Subscribe
  public void onTitleChanged(OnTitleChangedEvent event) {
    if (!menuDrawer.isMenuVisible()) {
      String title = ((FragmentContract) stack.getTopFragment()).getTitle();
      if (title != null) {
        activity.getActionBar().setTitle(title);
      } else {
        activity.getActionBar().setTitle(R.string.app_name);
      }
      activity.getActionBar()
          .setSubtitle(((FragmentContract) stack.getTopFragment()).getSubtitle());
    }
  }

  @Override
  public boolean onBackClicked() {
    final int drawerState = menuDrawer.getDrawerState();
    if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
      menuDrawer.closeMenu();
      return true;
    }

    if (searchView != null) {
      activity.getActionBar().setDisplayShowCustomEnabled(false);
      activity.getActionBar().setCustomView(null);
      stack.getTopFragment().setMenuVisibility(true);
      searchView = null;
      return true;
    }

    final FragmentContract topFragment = (FragmentContract) stack.getTopFragment();
    if (topFragment != null && topFragment.onBackPressed()) {
      return true;
    }

    if (stack.popStack(true)) {
      return true;
    }

    return false;
  }

  @Override
  public Bundle onSaveInstanceState() {
    Bundle state = new Bundle();
    stack.onSaveInstanceState(state);
    if (searchView != null) {
      state.putInt(STATE_SEARCH_TYPE, searchType);
      state.putCharSequence(STATE_SEARCH_QUERY, searchView.getQuery());
    }
    return state;
  }

  @Override
  public void onAttach() {
    super.onAttach();
    menuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_BEZEL);
    menuDrawer.setDrawerIndicatorEnabled(stack.getStackSize() == 1);

    activity.getActionBar().setHomeButtonEnabled(true);
    activity.getActionBar().setDisplayHomeAsUpEnabled(true);

    if (stack.getStackSize() == 0) {
      stack.setTopFragment(UpcomingShowsFragment.class, FRAGMENT_SHOWS_UPCOMING);
      stack.commit();
    }

    bus.register(this);
  }

  @Override
  public void onDetach() {
    bus.unregister(this);
    super.onDetach();
  }

  @Override
  public void onHomeClicked() {
    super.onHomeClicked();

    final int drawerState = menuDrawer.getDrawerState();
    if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
      menuDrawer.closeMenu();
    } else if (searchView != null) {
      activity.getActionBar().setDisplayShowCustomEnabled(false);
      activity.getActionBar().setCustomView(null);
      stack.getTopFragment().setMenuVisibility(true);
      searchView = null;
    } else if (!stack.popStack(drawerState == MenuDrawer.STATE_CLOSED)) {
      menuDrawer.toggleMenu();
    }
  }

  @Override
  public void onMenuItemClicked(int id) {
    switch (id) {
      case R.id.menu_shows_upcoming:
        stack.setTopFragment(UpcomingShowsFragment.class, FRAGMENT_SHOWS_UPCOMING);
        break;

      case R.id.menu_shows_watched:
        stack.setTopFragment(WatchedShowsFragment.class, FRAGMENT_SHOWS);
        break;

      case R.id.menu_shows_collection:
        stack.setTopFragment(ShowsCollectionFragment.class, FRAGMENT_SHOWS_COLLECTION);
        break;

      case R.id.menu_shows_watchlist:
        stack.setTopFragment(ShowsWatchlistFragment.class, FRAGMENT_SHOWS_WATCHLIST);
        break;

      case R.id.menu_episodes_watchlist:
        stack.setTopFragment(EpisodesWatchlistFragment.class, FRAGMENT_EPISODES_WATCHLIST);
        break;

      // case R.id.menu_shows_ratings:
      // case R.id.menu_shows_charts:

      case R.id.menu_movies_watched:
        stack.setTopFragment(WatchedMoviesFragment.class, FRAGMENT_MOVIES_WATCHED);
        break;

      case R.id.menu_movies_collection:
        stack.setTopFragment(MovieCollectionFragment.class, FRAGMENT_MOVIES_COLLECTION);
        break;

      case R.id.menu_movies_watchlist:
        stack.setTopFragment(MovieWatchlistFragment.class, FRAGMENT_MOVIES_WATCHLIST);
        break;

      // case R.id.menu_movies_ratings:
      // case R.id.menu_movies_charts:
    }

    menuDrawer.closeMenu();
  }

  private void createSearchView(int searchType) {
    this.searchType = searchType;
    searchView = (SearchView) LayoutInflater.from(activity.getActionBar().getThemedContext())
        .inflate(R.layout.search, null);
    searchView.onActionViewExpanded();
    activity.getActionBar().setDisplayShowCustomEnabled(true);
    activity.getActionBar()
        .setCustomView(searchView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
    stack.getTopFragment().setMenuVisibility(false);
    menuDrawer.setDrawerIndicatorEnabled(false);
    searchView.setOnCloseListener(new SearchView.OnCloseListener() {
      @Override
      public boolean onClose() {
        searchView = null;
        stack.getTopFragment().setMenuVisibility(true);
        return true;
      }
    });
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        LogWrapper.v(TAG, "[onQueryTextSubmit] Query: " + query);
        if (PhoneController.this.searchType == SEARCH_TYPE_MOVIE) {
          SearchMovieFragment f = stack.getFragment(FRAGMENT_SEARCH_MOVIE);
          if (f == null) {
            stack.addFragment(SearchMovieFragment.class, FRAGMENT_SEARCH_MOVIE,
                SearchMovieFragment.getArgs(query));
            stack.commit();
          } else {
            f.query(query);
          }
        } else {
          SearchShowFragment f = stack.getFragment(FRAGMENT_SEARCH_SHOW);
          if (f == null) {
            stack.addFragment(SearchShowFragment.class, FRAGMENT_SEARCH_SHOW,
                SearchShowFragment.getArgs(query));
            stack.commit();
          } else {
            f.query(query);
          }
        }

        searchView.clearFocus();

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
    stack.addFragment(ShowInfoFragment.class, FRAGMENT_SHOW,
        ShowInfoFragment.getArgs(showId, title, type));
    stack.commit();
  }

  @Override
  public void onDisplayEpisode(long episodeId, String showTitle) {
    Bundle args = EpisodeFragment.getArgs(episodeId, showTitle);
    if (isTablet) {
      EpisodeFragment f =
          (EpisodeFragment) Fragment.instantiate(activity, EpisodeFragment.class.getName(), args);
      f.show(activity.getSupportFragmentManager(), FRAGMENT_EPISODE);
    } else {
      stack.addFragment(EpisodeFragment.class, FRAGMENT_EPISODE,
          EpisodeFragment.getArgs(episodeId, showTitle));
      stack.commit();
    }
  }

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    stack.addFragment(SeasonFragment.class, FRAGMENT_SEASON,
        SeasonFragment.getArgs(showId, seasonId, showTitle, seasonNumber, type));
    stack.commit();
  }

  @Override
  public void onStartShowSearch() {
    super.onStartShowSearch();
    createSearchView(SEARCH_TYPE_SHOW);
  }

  @Override
  public void onDisplayMovie(long movieId, String title) {
    stack.addFragment(MovieFragment.class, FRAGMENT_MOVIE, MovieFragment.getArgs(movieId, title));
    stack.commit();
  }

  @Override
  public void onStartMovieSearch() {
    super.onStartMovieSearch();
    createSearchView(SEARCH_TYPE_MOVIE);
  }
}
