package net.simonvt.cathode.ui;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ProgressBar;
import android.widget.SearchView;
import butterknife.InjectView;
import butterknife.Views;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.OnTitleChangedEvent;
import net.simonvt.cathode.event.SyncEvent;
import net.simonvt.cathode.ui.fragment.EpisodeFragment;
import net.simonvt.cathode.ui.fragment.EpisodesWatchlistFragment;
import net.simonvt.cathode.ui.fragment.MovieCollectionFragment;
import net.simonvt.cathode.ui.fragment.MovieFragment;
import net.simonvt.cathode.ui.fragment.MovieWatchlistFragment;
import net.simonvt.cathode.ui.fragment.NavigationFragment;
import net.simonvt.cathode.ui.fragment.SearchMovieFragment;
import net.simonvt.cathode.ui.fragment.SearchShowFragment;
import net.simonvt.cathode.ui.fragment.SeasonFragment;
import net.simonvt.cathode.ui.fragment.ShowInfoFragment;
import net.simonvt.cathode.ui.fragment.ShowsCollectionFragment;
import net.simonvt.cathode.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.fragment.TrendingMoviesFragment;
import net.simonvt.cathode.ui.fragment.TrendingShowsFragment;
import net.simonvt.cathode.ui.fragment.UpcomingShowsFragment;
import net.simonvt.cathode.ui.fragment.WatchedMoviesFragment;
import net.simonvt.cathode.ui.fragment.WatchedShowsFragment;
import net.simonvt.cathode.util.FragmentStack;
import net.simonvt.cathode.util.LogWrapper;
import net.simonvt.menudrawer.MenuDrawer;

public class PhoneController extends UiController {

  private static final String TAG = "PhoneController";

  private static final String STATE_SEARCH_TYPE =
      "net.simonvt.cathode.ui.PhoneController.searchType";
  private static final String STATE_SEARCH_QUERY =
      "net.simonvt.cathode.ui.PhoneController.searchQuery";

  private static final int SEARCH_TYPE_SHOW = 1;
  private static final int SEARCH_TYPE_MOVIE = 2;

  @Inject Bus bus;

  private FragmentStack stack;

  @InjectView(R.id.drawer) MenuDrawer menuDrawer;

  @InjectView(R.id.progress_top) ProgressBar progressTop;
  private ViewPropertyAnimator progressAnimator;

  private NavigationFragment navigation;

  private int searchType;
  private SearchView searchView;

  private boolean isTablet;

  public static PhoneController newInstance(HomeActivity activity) {
    return new PhoneController(activity);
  }

  PhoneController(HomeActivity activity) {
    super(activity);
    CathodeApp.inject(activity, this);
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    isTablet = activity.getResources().getBoolean(R.bool.isTablet);

    Views.inject(this, activity);

    progressTop.setVisibility(View.GONE);

    menuDrawer.setupUpIndicator(activity);
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
    stack.setDefaultAnimation(R.anim.fade_in_front, R.anim.fade_out_back, R.anim.fade_in_back,
        R.anim.fade_out_front);

    menuDrawer.setOnDrawerStateChangeListener(new MenuDrawer.OnDrawerStateChangeListener() {
      @Override
      public void onDrawerStateChange(int oldState, int newState) {
        switch (newState) {
          case MenuDrawer.STATE_CLOSED:
            if (!stack.commit()) {
              String title = ((FragmentContract) stack.peek()).getTitle();
              if (title != null) {
                activity.getActionBar().setTitle(title);
              } else {
                activity.getActionBar().setTitle(R.string.app_name);
              }
              activity.getActionBar().setSubtitle(((FragmentContract) stack.peek()).getSubtitle());
            }

            if (searchView != null) {
              activity.getActionBar().setDisplayShowCustomEnabled(true);
            } else {
              stack.peek().setMenuVisibility(true);
            }
            activity.setMenuVisibility(true);
            break;

          default:
            stack.peek().setMenuVisibility(false);
            activity.setMenuVisibility(false);
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
      stack.restoreState(state);
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
      String title = ((FragmentContract) stack.peek()).getTitle();
      if (title != null) {
        activity.getActionBar().setTitle(title);
      } else {
        activity.getActionBar().setTitle(R.string.app_name);
      }
      activity.getActionBar().setSubtitle(((FragmentContract) stack.peek()).getSubtitle());
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
      stack.peek().setMenuVisibility(true);
      searchView = null;
      return true;
    }

    final FragmentContract topFragment = (FragmentContract) stack.peek();
    if (topFragment != null && topFragment.onBackPressed()) {
      return true;
    }

    if (stack.pop(true)) {
      return true;
    }

    return false;
  }

  @Override
  public Bundle onSaveInstanceState() {
    Bundle state = new Bundle();
    stack.saveState(state);
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
    menuDrawer.setDrawerIndicatorEnabled(stack.size() == 1);

    activity.getActionBar().setHomeButtonEnabled(true);
    activity.getActionBar().setDisplayHomeAsUpEnabled(true);

    if (stack.size() == 0) {
      stack.replace(UpcomingShowsFragment.class, FRAGMENT_SHOWS_UPCOMING);
      stack.commit();
    }

    bus.register(this);
  }

  @Override
  public void onDetach() {
    bus.unregister(this);
    super.onDetach();
  }

  @Override public void onDestroy(boolean completely) {
    if (completely) {
      stack.destroy();
    }
    super.onDestroy(completely);
  }

  @Subscribe
  public void onSyncEvent(SyncEvent event) {
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

  @Override
  public void onHomeClicked() {
    super.onHomeClicked();

    final int drawerState = menuDrawer.getDrawerState();
    if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
      menuDrawer.closeMenu();
    } else if (searchView != null) {
      activity.getActionBar().setDisplayShowCustomEnabled(false);
      activity.getActionBar().setCustomView(null);
      stack.peek().setMenuVisibility(true);
      searchView = null;
    } else if (!stack.pop(drawerState == MenuDrawer.STATE_CLOSED)) {
      menuDrawer.toggleMenu();
    }
  }

  @Override
  public void onMenuItemClicked(int id) {
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

      case R.id.menu_episodes_watchlist:
        stack.replace(EpisodesWatchlistFragment.class, FRAGMENT_EPISODES_WATCHLIST);
        break;

      case R.id.menu_shows_trending:
        stack.replace(TrendingShowsFragment.class, FRAGMENT_SHOWS_TRENDING);
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
    stack.peek().setMenuVisibility(false);
    menuDrawer.setDrawerIndicatorEnabled(false);
    searchView.setOnCloseListener(new SearchView.OnCloseListener() {
      @Override
      public boolean onClose() {
        searchView = null;
        stack.peek().setMenuVisibility(true);
        return true;
      }
    });
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        LogWrapper.v(TAG, "[onQueryTextSubmit] Query: " + query);
        if (PhoneController.this.searchType == SEARCH_TYPE_MOVIE) {
          SearchMovieFragment f = (SearchMovieFragment) activity.getSupportFragmentManager()
              .findFragmentByTag(FRAGMENT_SEARCH_MOVIE);
          if (f == null) {
            stack.push(SearchMovieFragment.class, FRAGMENT_SEARCH_MOVIE,
                SearchMovieFragment.getArgs(query));
            stack.commit();
          } else {
            f.query(query);
          }
        } else {
          SearchShowFragment f = (SearchShowFragment) activity.getSupportFragmentManager()
              .findFragmentByTag(FRAGMENT_SEARCH_SHOW);
          if (f == null) {
            stack.push(SearchShowFragment.class, FRAGMENT_SEARCH_SHOW,
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
    stack.push(ShowInfoFragment.class, FRAGMENT_SHOW,
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

  @Override
  public void onStartShowSearch() {
    super.onStartShowSearch();
    createSearchView(SEARCH_TYPE_SHOW);
  }

  @Override
  public void onDisplayMovie(long movieId, String title) {
    stack.push(MovieFragment.class, FRAGMENT_MOVIE, MovieFragment.getArgs(movieId, title));
    stack.commit();
  }

  @Override
  public void onStartMovieSearch() {
    super.onStartMovieSearch();
    createSearchView(SEARCH_TYPE_MOVIE);
  }
}
