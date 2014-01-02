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

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.OnTitleChangedEvent;
import net.simonvt.cathode.event.SyncEvent;
import net.simonvt.cathode.scheduler.SearchTaskScheduler;
import net.simonvt.cathode.ui.adapter.MovieSuggestionAdapter;
import net.simonvt.cathode.ui.adapter.ShowSuggestionAdapter;
import net.simonvt.cathode.ui.adapter.SuggestionsAdapter;
import net.simonvt.cathode.ui.fragment.EpisodeFragment;
import net.simonvt.cathode.ui.fragment.EpisodesWatchlistFragment;
import net.simonvt.cathode.ui.fragment.MovieCollectionFragment;
import net.simonvt.cathode.ui.fragment.MovieFragment;
import net.simonvt.cathode.ui.fragment.MovieRecommendationsFragment;
import net.simonvt.cathode.ui.fragment.MovieWatchlistFragment;
import net.simonvt.cathode.ui.fragment.NavigationFragment;
import net.simonvt.cathode.ui.fragment.SearchMovieFragment;
import net.simonvt.cathode.ui.fragment.SearchShowFragment;
import net.simonvt.cathode.ui.fragment.SeasonFragment;
import net.simonvt.cathode.ui.fragment.ShowFragment;
import net.simonvt.cathode.ui.fragment.ShowRecommendationsFragment;
import net.simonvt.cathode.ui.fragment.ShowsCollectionFragment;
import net.simonvt.cathode.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.fragment.TrendingMoviesFragment;
import net.simonvt.cathode.ui.fragment.TrendingShowsFragment;
import net.simonvt.cathode.ui.fragment.UpcomingShowsFragment;
import net.simonvt.cathode.ui.fragment.WatchedMoviesFragment;
import net.simonvt.cathode.ui.fragment.WatchedShowsFragment;
import net.simonvt.cathode.util.FragmentStack;
import net.simonvt.cathode.widget.SearchView;
import net.simonvt.menudrawer.MenuDrawer;
import timber.log.Timber;

public class PhoneController extends UiController {

  private static final String TAG = "PhoneController";

  private static final String STATE_SEARCH_TYPE =
      "net.simonvt.cathode.ui.PhoneController.searchType";
  private static final String STATE_SEARCH_QUERY =
      "net.simonvt.cathode.ui.PhoneController.searchQuery";

  private static final int SEARCH_TYPE_SHOW = 1;
  private static final int SEARCH_TYPE_MOVIE = 2;

  @Inject Bus bus;
  @Inject SearchTaskScheduler searchScheduler;

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

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    isTablet = activity.getResources().getBoolean(R.bool.isTablet);

    ButterKnife.inject(this, activity);

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
      @Override public void onStackChanged(int stackSize, Fragment topFragment) {
        Timber.d("onStackChanged: %s", topFragment.getTag());
        FragmentContract fragment = (FragmentContract) topFragment;

        menuDrawer.setDrawerIndicatorEnabled(stackSize <= 1);
        if (!menuDrawer.isMenuVisible()) {
          updateTitle();
        }
        if (searchView != null) {
          if (!FRAGMENT_SEARCH_MOVIE.equals(topFragment.getTag()) && !FRAGMENT_SEARCH_SHOW.equals(
              topFragment.getTag())) {
            destroySearchView();
          }
        }
        topFragment.setMenuVisibility(searchView == null);
      }
    });
    stack.setDefaultAnimation(R.anim.fade_in_front, R.anim.fade_out_back, R.anim.fade_in_back,
        R.anim.fade_out_front);

    menuDrawer.setOnDrawerStateChangeListener(new MenuDrawer.OnDrawerStateChangeListener() {
      @Override public void onDrawerStateChange(int oldState, int newState) {
        switch (newState) {
          case MenuDrawer.STATE_CLOSED:
            if (!stack.commit()) {
              updateTitle();
            }

            if (searchView != null) {
              activity.getActionBar().setDisplayShowCustomEnabled(true);
              activity.getActionBar().setDisplayShowTitleEnabled(false);
              searchView.requestFocus();
            } else {
              stack.peek().setMenuVisibility(true);
            }
            activity.setMenuVisibility(true);
            break;

          case MenuDrawer.STATE_OPEN:
            InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
              imm.hideSoftInputFromWindow(menuDrawer.getWindowToken(), 0);
            }

          default:
            stack.peek().setMenuVisibility(false);
            activity.setMenuVisibility(false);
            activity.getActionBar().setDisplayShowCustomEnabled(false);
            activity.getActionBar().setDisplayShowTitleEnabled(true);
            activity.getActionBar().setTitle(R.string.app_name);
            break;
        }
      }

      @Override public void onDrawerSlide(float openRatio, int offsetPixels) {
      }
    });

    if (inState != null) {
      stack.restoreState(inState);
      CharSequence query = inState.getCharSequence(STATE_SEARCH_QUERY);
      if (query != null) {
        searchType = inState.getInt(STATE_SEARCH_TYPE);
        if (searchType == SEARCH_TYPE_SHOW) {
          onStartShowSearch();
        } else {
          onStartMovieSearch();
        }
        searchView.setQuery(query);
      }
    }
  }

  @Subscribe public void onTitleChanged(OnTitleChangedEvent event) {
    if (!menuDrawer.isMenuVisible()) {
      updateTitle();
    }
  }

  private void updateTitle() {
    Fragment f = stack.peek();
    if (f.isAdded() && !f.isDetached()) {
      String title = ((FragmentContract) f).getTitle();
      if (title != null) {
        activity.getActionBar().setTitle(title);
      } else {
        activity.getActionBar().setTitle(R.string.app_name);
      }
      activity.getActionBar().setSubtitle(((FragmentContract) f).getSubtitle());
    }
  }

  @Override public boolean onBackClicked() {
    final int drawerState = menuDrawer.getDrawerState();
    if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
      menuDrawer.closeMenu();
      return true;
    }

    if (searchView != null) {
      destroySearchView();
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

  @Override public Bundle onSaveInstanceState() {
    Bundle outState = new Bundle();
    stack.saveState(outState);
    if (searchView != null) {
      outState.putInt(STATE_SEARCH_TYPE, searchType);
      outState.putCharSequence(STATE_SEARCH_QUERY, searchView.getQuery());
    }
    return outState;
  }

  @Override public void onAttach() {
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

  @Override public void onDetach() {
    super.onDetach();
  }

  @Override public void onDestroy(boolean completely) {
    if (completely) {
      stack.destroy();
    }
    bus.unregister(this);
    super.onDestroy(completely);
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

  @Override public void onHomeClicked() {
    super.onHomeClicked();

    final int drawerState = menuDrawer.getDrawerState();
    if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
      menuDrawer.closeMenu();
    } else if (searchView != null) {
      destroySearchView();
    } else if (!stack.pop(drawerState == MenuDrawer.STATE_CLOSED)) {
      menuDrawer.toggleMenu();
    }
  }

  @Override public void onMenuItemClicked(int id) {
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

      case R.id.menu_shows_recommendations:
        stack.replace(ShowRecommendationsFragment.class, FRAGMENT_SHOWS_RECOMMENDATIONS);
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

      case R.id.menu_movies_recommendations:
        stack.replace(MovieRecommendationsFragment.class, FRAGMENT_MOVIES_RECOMMENDATIONS);
        break;

      default:
        throw new IllegalArgumentException("Unknown id " + id);
    }

    if (searchView != null) destroySearchView();

    menuDrawer.closeMenu();
  }

  private void createSearchView(int searchType) {
    this.searchType = searchType;
    searchView = (SearchView) LayoutInflater.from(activity.getActionBar().getThemedContext())
        .inflate(R.layout.search_view, null);
    activity.getActionBar().setDisplayShowCustomEnabled(true);
    activity.getActionBar()
        .setCustomView(searchView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
    activity.getActionBar().setDisplayShowTitleEnabled(false);
    stack.peek().setMenuVisibility(false);
    menuDrawer.setDrawerIndicatorEnabled(false);
    searchView.setListener(new SearchView.SearchViewListener() {
      @Override public void onTextChanged(String newText) {
      }

      @Override public void onSubmit(String query) {
        Timber.d("[onQueryTextSubmit] Query: %s", query);
        if (PhoneController.this.searchType == SEARCH_TYPE_MOVIE) {
          SearchMovieFragment f = (SearchMovieFragment) activity.getSupportFragmentManager()
              .findFragmentByTag(FRAGMENT_SEARCH_MOVIE);
          if (f == null) {
            stack.push(SearchMovieFragment.class, FRAGMENT_SEARCH_MOVIE,
                SearchMovieFragment.getArgs(query));
            stack.executePendingTransactions();
          } else {
            f.query(query);
          }

          searchScheduler.insertMovieQuery(query);
        } else {
          SearchShowFragment f = (SearchShowFragment) activity.getSupportFragmentManager()
              .findFragmentByTag(FRAGMENT_SEARCH_SHOW);
          if (f == null) {
            stack.push(SearchShowFragment.class, FRAGMENT_SEARCH_SHOW,
                SearchShowFragment.getArgs(query));
            stack.executePendingTransactions();
          } else {
            f.query(query);
          }

          searchScheduler.insertShowQuery(query);
        }

        destroySearchView();
      }

      @Override public void onSuggestionSelected(Object suggestion) {
        SuggestionsAdapter.Suggestion item = (SuggestionsAdapter.Suggestion) suggestion;
        if (PhoneController.this.searchType == SEARCH_TYPE_MOVIE) {
          if (item.getId() != null) {
            onDisplayMovie(item.getId(), item.getTitle());
          } else {
            SearchMovieFragment f = (SearchMovieFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_SEARCH_MOVIE);
            if (f == null) {
              stack.push(SearchMovieFragment.class, FRAGMENT_SEARCH_MOVIE,
                  SearchMovieFragment.getArgs(item.getTitle()));
              stack.executePendingTransactions();
            } else {
              f.query(item.getTitle());
            }
          }
        } else {
          if (item.getId() != null) {
            onDisplayShow(item.getId(), item.getTitle(), LibraryType.WATCHED);
          } else {
            SearchShowFragment f = (SearchShowFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_SEARCH_SHOW);
            if (f == null) {
              stack.push(SearchShowFragment.class, FRAGMENT_SEARCH_SHOW,
                  SearchShowFragment.getArgs(item.getTitle()));
              stack.executePendingTransactions();
            } else {
              f.query(item.getTitle());
            }
          }
        }

        destroySearchView();
      }
    });
  }

  private void destroySearchView() {
    searchView.clearFocus();
    searchView = null;
    activity.getActionBar().setCustomView(null);
    activity.getActionBar().setDisplayShowCustomEnabled(false);
    activity.getActionBar().setDisplayShowTitleEnabled(true);
    if (stack.size() <= 1) {
      menuDrawer.setDrawerIndicatorEnabled(true);
    }
    stack.peek().setMenuVisibility(true);
    updateTitle();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Navigation callbacks
  ///////////////////////////////////////////////////////////////////////////

  @Override public void onDisplayShow(long showId, String title, LibraryType type) {
    stack.push(ShowFragment.class, FRAGMENT_SHOW, ShowFragment.getArgs(showId, title, type));
    stack.commit();
  }

  @Override public void onDisplayEpisode(long episodeId, String showTitle) {
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

  @Override public void onStartShowSearch() {
    super.onStartShowSearch();
    createSearchView(SEARCH_TYPE_SHOW);
    searchView.setAdapter(new ShowSuggestionAdapter(activity.getActionBar().getThemedContext()));
  }

  @Override public void onDisplayMovie(long movieId, String title) {
    stack.push(MovieFragment.class, FRAGMENT_MOVIE, MovieFragment.getArgs(movieId, title));
    stack.commit();
  }

  @Override public void onStartMovieSearch() {
    super.onStartMovieSearch();
    createSearchView(SEARCH_TYPE_MOVIE);
    searchView.setAdapter(new MovieSuggestionAdapter(activity.getActionBar().getThemedContext()));
  }
}
