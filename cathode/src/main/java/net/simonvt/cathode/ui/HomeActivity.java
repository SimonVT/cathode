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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.AndroidInjection;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.entity.ShowWithEpisode;
import net.simonvt.cathode.common.event.AuthFailedEvent;
import net.simonvt.cathode.common.event.AuthFailedEvent.OnAuthFailedListener;
import net.simonvt.cathode.common.event.ErrorEvent;
import net.simonvt.cathode.common.event.ErrorEvent.ErrorListener;
import net.simonvt.cathode.common.event.RequestFailedEvent;
import net.simonvt.cathode.common.event.RequestFailedEvent.OnRequestFailedListener;
import net.simonvt.cathode.common.event.SyncEvent;
import net.simonvt.cathode.common.event.SyncEvent.OnSyncListener;
import net.simonvt.cathode.common.ui.FragmentContract;
import net.simonvt.cathode.common.util.FragmentStack;
import net.simonvt.cathode.common.util.FragmentStack.StackEntry;
import net.simonvt.cathode.common.util.MainHandler;
import net.simonvt.cathode.common.widget.Crouton;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.settings.LinkPromptBottomSheet;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.SettingsActivity;
import net.simonvt.cathode.settings.SetupPromptBottomSheet;
import net.simonvt.cathode.settings.StartPage;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.login.LoginActivity;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.comments.CommentFragment;
import net.simonvt.cathode.ui.comments.CommentsFragment;
import net.simonvt.cathode.ui.credits.CreditFragment;
import net.simonvt.cathode.ui.credits.CreditsFragment;
import net.simonvt.cathode.ui.dashboard.DashboardFragment;
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment;
import net.simonvt.cathode.ui.lists.ListFragment;
import net.simonvt.cathode.ui.lists.ListsFragment;
import net.simonvt.cathode.ui.movie.MovieFragment;
import net.simonvt.cathode.ui.movie.MovieHistoryFragment;
import net.simonvt.cathode.ui.movie.RelatedMoviesFragment;
import net.simonvt.cathode.ui.movies.collected.CollectedMoviesFragment;
import net.simonvt.cathode.ui.movies.watched.WatchedMoviesFragment;
import net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment;
import net.simonvt.cathode.ui.navigation.NavigationFragment;
import net.simonvt.cathode.ui.person.PersonCreditsFragment;
import net.simonvt.cathode.ui.person.PersonFragment;
import net.simonvt.cathode.ui.search.SearchFragment;
import net.simonvt.cathode.ui.show.EpisodeFragment;
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment;
import net.simonvt.cathode.ui.show.RelatedShowsFragment;
import net.simonvt.cathode.ui.show.SeasonFragment;
import net.simonvt.cathode.ui.show.ShowFragment;
import net.simonvt.cathode.ui.shows.collected.CollectedShowsFragment;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment;
import net.simonvt.cathode.ui.shows.watched.WatchedShowsFragment;
import net.simonvt.cathode.ui.shows.watchlist.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.stats.StatsFragment;
import net.simonvt.cathode.ui.suggestions.movies.MovieSuggestionsFragment;
import net.simonvt.cathode.ui.suggestions.shows.ShowSuggestionsFragment;
import net.simonvt.cathode.widget.WatchingView;
import net.simonvt.cathode.widget.WatchingView.WatchingViewListener;
import timber.log.Timber;

public class HomeActivity extends BaseActivity
    implements NavigationFragment.OnMenuClickListener, NavigationListener,
    LinkPromptBottomSheet.LinkPromptDismissListener {

  private class PendingReplacement {

    Class fragment;

    String tag;

    PendingReplacement(Class fragment, String tag) {
      this.fragment = fragment;
      this.tag = tag;
    }
  }

  public static final String DIALOG_ABOUT = "net.simonvt.cathode.ui.BaseActivity.aboutDialog";
  public static final String DIALOG_LOGOUT = "net.simonvt.cathode.ui.HomeActivity.logoutDialog";

  private static final String PROMPT_LINK = "net.simonvt.cathode.ui.HomeActivity.linkPrompt";
  private static final String PROMPT_SETUP = "net.simonvt.cathode.ui.HomeActivity.setupPrompt";

  private static final String STATE_STACK = "net.simonvt.cathode.ui.HomeActivity.stack";

  public static final String EXTRA_START_PAGE = "net.simonvt.cathode.ui.HomeActivity.startPage";
  public static final String EXTRA_STACK_ENTRIES =
      "net.simonvt.cathode.ui.HomeActivity.stackEntries";

  public static final String ACTION_CONSUMED = "consumed";
  public static final String ACTION_LOGIN = "net.simonvt.cathode.intent.action.LOGIN";
  public static final String ACTION_SHOW_START_PAGE =
      "net.simonvt.cathode.intent.action.showStartPage";
  public static final String ACTION_REPLACE_STACK = "replaceStack";
  public static final String ACTION_SEARCH = "net.simonvt.cathode.SEARCH";
  public static final String ACTION_UPCOMING = "net.simonvt.cathode.UPCOMING";

  @Inject ShowTaskScheduler showScheduler;
  @Inject MovieTaskScheduler movieScheduler;

  @BindView(R.id.progress_top) ProgressBar progressTop;

  @BindView(R.id.crouton) Crouton crouton;

  private FragmentStack stack;

  @BindView(R.id.drawer) DrawerLayout drawer;
  private NavigationFragment navigation;

  @BindView(R.id.watching_parent) ViewGroup watchingParent;
  @BindView(R.id.watchingView) WatchingView watchingView;

  @BindView(R.id.authFailedView) View authFailedView;
  @BindView(R.id.authFailedAction) View authFailedAction;

  private HomeViewModel viewModel;

  private ShowWithEpisode watchingShow;
  private Movie watchingMovie;

  private PendingReplacement pendingReplacement;

  private boolean isSyncing = false;

  @Override protected void onCreate(@Nullable Bundle inState) {
    setTheme(R.style.Theme);
    super.onCreate(inState);
    Timber.d("onCreate");
    AndroidInjection.inject(this);

    setContentView(R.layout.activity_home);

    ButterKnife.bind(this);
    drawer.addDrawerListener(drawerListener);
    watchingParent.setOnTouchListener(watchingTouchListener);
    watchingView.setWatchingViewListener(watchingListener);

    authFailedAction.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startLoginActivity();
      }
    });

    navigation =
        (NavigationFragment) getSupportFragmentManager().findFragmentByTag(NavigationFragment.TAG);

    stack = FragmentStack.forContainer(this, R.id.content);
    stack.setDefaultAnimation(R.anim.fade_in_front, R.anim.fade_out_back, R.anim.fade_in_back,
        R.anim.fade_out_front);
    if (inState != null) {
      stack.restoreState(inState.getBundle(STATE_STACK));
    }

    final Intent intent = getIntent();

    if (isShowStartPageIntent(intent)) {
      StartPage startPage = (StartPage) intent.getSerializableExtra(EXTRA_START_PAGE);
      if (startPage == null) {
        startPage = StartPage.SHOWS_UPCOMING;
      }
      navigation.setSelectedId(startPage.getMenuId());
      stack.replace(startPage.getPageClass(), startPage.getTag());
    } else if (isShowUpcomingAction(intent)) {
      navigation.setSelectedId(StartPage.SHOWS_UPCOMING.getMenuId());
      stack.replace(StartPage.SHOWS_UPCOMING.getPageClass(), StartPage.SHOWS_UPCOMING.getTag());
    } else {
      if (stack.size() == 0) {
        final String startPagePref = Settings.get(this).getString(Settings.START_PAGE, null);
        StartPage startPage = StartPage.fromValue(startPagePref, StartPage.DASHBOARD);
        navigation.setSelectedId(startPage.getMenuId());
        stack.replace(startPage.getPageClass(), startPage.getTag());
      }

      if (isSearchAction(intent)) {
        onSearchClicked();
      }
    }

    if (!TraktLinkSettings.isLinkPrompted(this)) {
      displayLinkPrompt();
    } else if (isLoginAction(getIntent())) {
      startLoginActivity();
    } else {
      if (isReplaceStackAction(intent)) {
        ArrayList<StackEntry> stackEntries =
            getIntent().getParcelableArrayListExtra(EXTRA_STACK_ENTRIES);
        replaceStack(stackEntries);
      }

      if (!Settings.get(this).getBoolean(Settings.SETUP_PROMPTED, false)) {
        displaySetupPrompt();
      }
    }

    intent.setAction(ACTION_CONSUMED);

    SyncEvent.registerListener(onSyncEvent);
    RequestFailedEvent.registerListener(requestFailedListener);
    ErrorEvent.registerListener(checkInFailedListener);
    AuthFailedEvent.registerListener(onAuthFailedListener);

    viewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
    viewModel.getWatchingShow().observe(this, new Observer<ShowWithEpisode>() {
      @Override public void onChanged(ShowWithEpisode showWithEpisode) {
        watchingShow = showWithEpisode;
        updateWatching();
      }
    });
    viewModel.getWatchingMovie().observe(this, new Observer<Movie>() {
      @Override public void onChanged(Movie movie) {
        watchingMovie = movie;
        updateWatching();
      }
    });
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (isLoginAction(intent)) {
      MainHandler.post(new Runnable() {
        @Override public void run() {
          startLoginActivity();
        }
      });
    } else if (isShowStartPageIntent(intent)) {
      final StartPage startPage = (StartPage) intent.getSerializableExtra(EXTRA_START_PAGE);
      MainHandler.post(new Runnable() {
        @Override public void run() {
          showStartPage(startPage);
        }
      });
    } else if (isReplaceStackAction(intent)) {
      final ArrayList<StackEntry> stackEntries =
          intent.getParcelableArrayListExtra(EXTRA_STACK_ENTRIES);
      MainHandler.post(new Runnable() {
        @Override public void run() {
          replaceStack(stackEntries);
        }
      });
    }

    intent.setAction(ACTION_CONSUMED);
  }

  private boolean isShowStartPageIntent(Intent intent) {
    return ACTION_SHOW_START_PAGE.equals(intent.getAction());
  }

  private void showStartPage(StartPage startPage) {
    navigation.setSelectedId(startPage.getMenuId());
    onMenuItemClicked(startPage.getMenuId());

    if (pendingReplacement != null) {
      stack.replace(pendingReplacement.fragment, pendingReplacement.tag);
      pendingReplacement = null;
    }
  }

  public void replaceStack(List<StackEntry> stackEntries) {
    Fragment f = stack.peekFirst();
    StackEntry entry = new StackEntry(f.getClass(), f.getTag(), f.getArguments());
    stackEntries.add(0, entry);
    stack.replaceStack(stackEntries);
  }

  private boolean isReplaceStackAction(Intent intent) {
    return ACTION_REPLACE_STACK.equals(intent.getAction());
  }

  private boolean isLoginAction(Intent intent) {
    return ACTION_LOGIN.equals(intent.getAction());
  }

  private boolean isSearchAction(Intent intent) {
    return ACTION_SEARCH.equals(intent.getAction());
  }

  private boolean isShowUpcomingAction(Intent intent) {
    return ACTION_UPCOMING.equals(intent.getAction());
  }

  private void displayLinkPrompt() {
    if (getSupportFragmentManager().findFragmentByTag(PROMPT_LINK) == null) {
      LinkPromptBottomSheet linkPrompt = new LinkPromptBottomSheet();
      linkPrompt.show(getSupportFragmentManager(), PROMPT_LINK);
    }
  }

  @Override public void onDismissLinkPrompt() {
    if (!Settings.get(this).getBoolean(Settings.SETUP_PROMPTED, false)) {
      displaySetupPrompt();
    }
  }

  private void displaySetupPrompt() {
    if (getSupportFragmentManager().findFragmentByTag(PROMPT_SETUP) == null) {
      SetupPromptBottomSheet setupPrompt = new SetupPromptBottomSheet();
      setupPrompt.show(getSupportFragmentManager(), PROMPT_SETUP);
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    outState.putBundle(STATE_STACK, stack.saveState());
    super.onSaveInstanceState(outState);
  }

  @Override protected void onResume() {
    super.onResume();
    if (TraktLinkSettings.hasAuthFailed(this)) {
      authFailedView.setVisibility(View.VISIBLE);
    } else {
      authFailedView.setVisibility(View.GONE);
    }
  }

  @Override protected void onDestroy() {
    Timber.d("onDestroy");
    SyncEvent.unregisterListener(onSyncEvent);
    RequestFailedEvent.unregisterListener(requestFailedListener);
    ErrorEvent.unregisterListener(checkInFailedListener);
    super.onDestroy();
  }

  private DrawerLayout.DrawerListener drawerListener = new DrawerLayout.DrawerListener() {
    @Override public void onDrawerSlide(View drawerView, float slideOffset) {
    }

    @Override public void onDrawerOpened(View drawerView) {
      pendingReplacement = null;
    }

    @Override public void onDrawerClosed(View drawerView) {
      if (pendingReplacement != null) {
        stack.replace(pendingReplacement.fragment, pendingReplacement.tag);
        pendingReplacement = null;
      }
    }

    @Override public void onDrawerStateChanged(int newState) {
      if (newState == DrawerLayout.STATE_DRAGGING) {
        pendingReplacement = null;
      }
    }
  };

  @Override public void onBackPressed() {
    if (watchingView.isExpanded()) {
      watchingView.collapse();
      return;
    }

    if (drawer.isDrawerVisible(Gravity.LEFT)) {
      drawer.closeDrawer(Gravity.LEFT);
      return;
    }

    final FragmentContract topFragment = (FragmentContract) stack.peek();
    if (topFragment != null && topFragment.onBackPressed()) {
      return;
    }

    if (stack.pop()) {
      return;
    }

    super.onBackPressed();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onHomeClicked();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override public boolean onMenuItemClicked(int id) {
    switch (id) {
      case R.id.menu_dashboard:
        pendingReplacement = new PendingReplacement(DashboardFragment.class, DashboardFragment.TAG);
        break;

      case R.id.menu_shows_upcoming:
        pendingReplacement =
            new PendingReplacement(UpcomingShowsFragment.class, UpcomingShowsFragment.TAG);
        break;

      case R.id.menu_shows_watched:
        pendingReplacement =
            new PendingReplacement(WatchedShowsFragment.class, WatchedShowsFragment.TAG);
        break;

      case R.id.menu_shows_collection:
        pendingReplacement =
            new PendingReplacement(CollectedShowsFragment.class, CollectedShowsFragment.TAG);
        break;

      case R.id.menu_shows_watchlist:
        pendingReplacement =
            new PendingReplacement(ShowsWatchlistFragment.class, ShowsWatchlistFragment.TAG);
        break;

      case R.id.menu_shows_suggestions:
        pendingReplacement =
            new PendingReplacement(ShowSuggestionsFragment.class, ShowSuggestionsFragment.TAG);
        break;

      case R.id.menu_movies_watched:
        pendingReplacement =
            new PendingReplacement(WatchedMoviesFragment.class, WatchedMoviesFragment.TAG);
        break;

      case R.id.menu_movies_collection:
        pendingReplacement =
            new PendingReplacement(CollectedMoviesFragment.class, CollectedMoviesFragment.TAG);
        break;

      case R.id.menu_movies_watchlist:
        pendingReplacement =
            new PendingReplacement(MovieWatchlistFragment.class, MovieWatchlistFragment.TAG);
        break;

      case R.id.menu_movies_suggestions:
        pendingReplacement =
            new PendingReplacement(MovieSuggestionsFragment.class, MovieSuggestionsFragment.TAG);
        break;

      case R.id.menu_lists:
        pendingReplacement = new PendingReplacement(ListsFragment.class, ListsFragment.TAG);
        break;

      case R.id.menu_stats:
        pendingReplacement = new PendingReplacement(StatsFragment.class, StatsFragment.TAG);
        break;

      case R.id.menu_settings:
        Intent settings = new Intent(this, SettingsActivity.class);
        startActivity(settings);
        return false;

      default:
        throw new IllegalArgumentException("Unknown id " + id);
    }

    drawer.closeDrawer(Gravity.LEFT);
    return true;
  }

  private View.OnTouchListener watchingTouchListener = new View.OnTouchListener() {
    @SuppressLint("ClickableViewAccessibility") @Override
    public boolean onTouch(View v, MotionEvent event) {
      if (watchingView.isExpanded()) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
          watchingView.collapse();
        }

        return true;
      }

      return false;
    }
  };

  private WatchingViewListener watchingListener = new WatchingViewListener() {
    @Override public void onExpand(WatchingView view) {
      Timber.d("onExpand");
    }

    @Override public void onCollapse(WatchingView view) {
      Timber.d("onCollapse");
    }

    @Override public void onEpisodeClicked(WatchingView view, long episodeId, String showTitle) {
      watchingView.collapse();

      Fragment top = stack.peek();
      if (top instanceof EpisodeFragment) {
        EpisodeFragment f = (EpisodeFragment) top;
        if (episodeId == f.getEpisodeId()) {
          return;
        }
      }

      onDisplayEpisode(episodeId, showTitle);
    }

    @Override
    public void onMovieClicked(WatchingView view, long id, String title, String overview) {
      watchingView.collapse();

      Fragment top = stack.peek();
      if (top instanceof MovieFragment) {
        MovieFragment f = (MovieFragment) top;
        if (id == f.getMovieId()) {
          return;
        }
      }

      onDisplayMovie(id, title, overview);
    }

    @Override public void onAnimatingIn(WatchingView view) {
    }

    @Override public void onAnimatingOut(WatchingView view) {
    }
  };

  private OnSyncListener onSyncEvent = new OnSyncListener() {
    @Override public void onSyncChanged(boolean authExecuting, boolean dataExecuting) {
      final boolean isSyncing = authExecuting || dataExecuting;
      if (isSyncing != HomeActivity.this.isSyncing) {
        HomeActivity.this.isSyncing = isSyncing;

        final int progressVisibility = progressTop.getVisibility();
        ViewPropertyAnimator progressAnimator = progressTop.animate();
        if (isSyncing) {
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
    }
  };

  private OnRequestFailedListener requestFailedListener = new OnRequestFailedListener() {
    @Override public void onRequestFailed(RequestFailedEvent event) {
      crouton.show(getString(event.getErrorMessage()),
          getResources().getColor(android.R.color.holo_red_dark));
    }
  };

  private ErrorListener checkInFailedListener = new ErrorListener() {
    @Override public void onError(String error) {
      crouton.show(error, getResources().getColor(android.R.color.holo_red_dark));
    }
  };

  private OnAuthFailedListener onAuthFailedListener = new OnAuthFailedListener() {
    @Override public void onAuthFailed() {
      authFailedView.setVisibility(View.VISIBLE);
    }
  };

  private void startLoginActivity() {
    Intent login = new Intent(this, LoginActivity.class);
    login.putExtra(LoginActivity.EXTRA_TASK, LoginActivity.TASK_TOKEN_REFRESH);
    startActivity(login);
    finish();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Navigation callbacks
  ///////////////////////////////////////////////////////////////////////////

  @Override public void onHomeClicked() {
    if (stack.size() == 1) {
      drawer.openDrawer(Gravity.LEFT);
      return;
    }

    stack.pop();
  }

  @Override public void onSearchClicked() {
    stack.push(SearchFragment.class, SearchFragment.TAG);
  }

  @Override
  public void onDisplayShow(long showId, String title, String overview, LibraryType type) {
    stack.push(ShowFragment.class, ShowFragment.getTag(showId),
        ShowFragment.getArgs(showId, title, overview, type));
  }

  @Override public void onDisplayEpisode(long episodeId, String showTitle) {
    stack.push(EpisodeFragment.class, EpisodeFragment.getTag(episodeId),
        EpisodeFragment.getArgs(episodeId, showTitle));
  }

  @Override public void onDisplayEpisodeHistory(long episodeId, String showTitle) {
    stack.push(EpisodeHistoryFragment.class, EpisodeHistoryFragment.getTag(episodeId),
        EpisodeHistoryFragment.getArgs(episodeId, showTitle));
  }

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    stack.push(SeasonFragment.class, SeasonFragment.TAG,
        SeasonFragment.getArgs(showId, seasonId, showTitle, seasonNumber, type));
  }

  @Override public void onDisplayRelatedShows(long showId, String title) {
    stack.push(RelatedShowsFragment.class, RelatedShowsFragment.getTag(showId),
        RelatedShowsFragment.getArgs(showId));
  }

  @Override public void onSelectShowWatchedDate(long showId, String title) {
    stack.push(SelectHistoryDateFragment.class, SelectHistoryDateFragment.TAG,
        SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.SHOW, showId, title));
  }

  @Override public void onSelectSeasonWatchedDate(long seasonId, String title) {
    stack.push(SelectHistoryDateFragment.class, SelectHistoryDateFragment.TAG,
        SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.SEASON, seasonId, title));
  }

  @Override public void onSelectEpisodeWatchedDate(long episodeId, String title) {
    stack.push(SelectHistoryDateFragment.class, SelectHistoryDateFragment.TAG,
        SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.EPISODE, episodeId,
            title));
  }

  @Override public void onSelectOlderEpisodeWatchedDate(long episodeId, String title) {
    stack.push(SelectHistoryDateFragment.class, SelectHistoryDateFragment.TAG,
        SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.EPISODE_OLDER, episodeId,
            title));
  }

  @Override public void onDisplayMovie(long movieId, String title, String overview) {
    stack.push(MovieFragment.class, MovieFragment.getTag(movieId),
        MovieFragment.getArgs(movieId, title, overview));
  }

  @Override public void onDisplayRelatedMovies(long movieId, String title) {
    stack.push(RelatedMoviesFragment.class, RelatedMoviesFragment.getTag(movieId),
        RelatedMoviesFragment.getArgs(movieId));
  }

  @Override public void onSelectMovieWatchedDate(long movieId, String title) {
    stack.push(SelectHistoryDateFragment.class, SelectHistoryDateFragment.TAG,
        SelectHistoryDateFragment.getArgs(SelectHistoryDateFragment.Type.MOVIE, movieId, title));
  }

  @Override public void onDisplayMovieHistory(long movieId, String title) {
    stack.push(MovieHistoryFragment.class, MovieHistoryFragment.getTag(movieId),
        MovieHistoryFragment.getArgs(movieId, title));
  }

  @Override public void onShowList(long listId, String listName) {
    stack.push(ListFragment.class, ListFragment.TAG, ListFragment.getArgs(listId, listName));
  }

  @Override public void onListDeleted(long listId) {
    Fragment top = stack.peek();
    if (top instanceof ListFragment) {
      ListFragment f = (ListFragment) top;
      if (listId == f.getListId()) {
        stack.pop();
      }
    }
  }

  @Override public void onDisplayComments(ItemType type, long itemId) {
    stack.push(CommentsFragment.class, CommentsFragment.TAG,
        CommentsFragment.getArgs(type, itemId));
  }

  @Override public void onDisplayComment(long commentId) {
    stack.push(CommentFragment.class, CommentFragment.TAG, CommentFragment.getArgs(commentId));
  }

  @Override public void onDisplayPerson(long personId) {
    stack.push(PersonFragment.class, PersonFragment.getTag(personId),
        PersonFragment.getArgs(personId));
  }

  @Override public void onDisplayPersonCredit(long personId, Department department) {
    stack.push(PersonCreditsFragment.class, PersonCreditsFragment.getTag(personId),
        PersonCreditsFragment.getArgs(personId, department));
  }

  @Override public void onDisplayCredit(ItemType itemType, long itemId, Department department) {
    stack.push(CreditFragment.class, CreditFragment.getTag(itemId),
        CreditFragment.getArgs(itemType, itemId, department));
  }

  @Override public void onDisplayCredits(ItemType itemType, long itemId, String title) {
    stack.push(CreditsFragment.class, CreditsFragment.getTag(itemId),
        CreditsFragment.getArgs(itemType, itemId, title));
  }

  @Override public void displayFragment(Class clazz, String tag) {
    stack.push(clazz, tag, null);
  }

  @Override public void upFromEpisode(long showId, String showTitle, long seasonId) {
    if (stack.removeTop()) {
      Fragment f = stack.peek();
      if (f instanceof ShowFragment && ((ShowFragment) f).getShowId() == showId) {
        stack.attachTop();
      } else if (seasonId >= 0
          && f instanceof SeasonFragment
          && ((SeasonFragment) f).getSeasonId() == seasonId) {
        stack.attachTop();
      } else {
        stack.putFragment(ShowFragment.class, ShowFragment.getTag(showId),
            ShowFragment.getArgs(showId, showTitle, null, LibraryType.WATCHED));
      }
    }
  }

  @Override public void popIfTop(Fragment fragment) {
    if (fragment == stack.peek()) {
      stack.pop();
    }
  }

  @Override public boolean isFragmentTopLevel(Fragment fragment) {
    return stack.positionInStack(fragment) == 0;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Watching view
  ///////////////////////////////////////////////////////////////////////////

  private void updateWatching() {
    if (watchingShow != null) {
      final long showId = watchingShow.getShow().getId();
      final String showTitle = watchingShow.getShow().getTitle();
      final int season = watchingShow.getEpisode().getSeason();
      final int episode = watchingShow.getEpisode().getEpisode();

      final long episodeId = watchingShow.getEpisode().getId();
      final String episodeTitle =
          DataHelper.getEpisodeTitle(this, watchingShow.getEpisode().getTitle(), season, episode,
              false);
      final long startTime = watchingShow.getEpisode().getCheckinStartedAt();
      final long endTime = watchingShow.getEpisode().getCheckinExpiresAt();

      final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, showId);

      watchingView.watchingShow(showId, showTitle, episodeId, episodeTitle, poster, startTime,
          endTime);
    } else if (watchingMovie != null) {
      final long id = watchingMovie.getId();
      final String title = watchingMovie.getTitle();
      final String overview = watchingMovie.getOverview();
      final long startTime = watchingMovie.getCheckinStartedAt();
      final long endTime = watchingMovie.getCheckinExpiresAt();

      final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, id);

      watchingView.watchingMovie(id, title, overview, poster, startTime, endTime);
    } else {
      watchingView.clearWatching();
    }
  }
}
