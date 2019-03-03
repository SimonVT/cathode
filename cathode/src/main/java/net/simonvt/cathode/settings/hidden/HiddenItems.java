/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.settings.hidden;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import dagger.android.support.AndroidSupportInjection;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.ui.FragmentContract;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.common.util.FragmentStack;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.SyncHiddenItems;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationClickListener;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.comments.CommentFragment;
import net.simonvt.cathode.ui.comments.CommentsFragment;
import net.simonvt.cathode.ui.credits.CreditFragment;
import net.simonvt.cathode.ui.credits.CreditsFragment;
import net.simonvt.cathode.ui.history.SelectHistoryDateFragment;
import net.simonvt.cathode.ui.lists.ListFragment;
import net.simonvt.cathode.ui.movie.MovieFragment;
import net.simonvt.cathode.ui.movie.MovieHistoryFragment;
import net.simonvt.cathode.ui.movie.RelatedMoviesFragment;
import net.simonvt.cathode.ui.person.PersonCreditsFragment;
import net.simonvt.cathode.ui.person.PersonFragment;
import net.simonvt.cathode.ui.show.EpisodeFragment;
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment;
import net.simonvt.cathode.ui.show.RelatedShowsFragment;
import net.simonvt.cathode.ui.show.SeasonFragment;
import net.simonvt.cathode.ui.show.ShowFragment;

public class HiddenItems extends BaseActivity
    implements NavigationClickListener, NavigationListener {

  private static final String FRAGMENT_HIDDEN =
      "net.simonvt.cathode.settings.hidden.HiddenItems.HiddenItemsFragment";

  private static final String STATE_STACK = "net.simonvt.cathode.ui.HomeActivity.stack";

  private FragmentStack stack;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.activity_hidden);

    stack = FragmentStack.forContainer(this, R.id.content);
    stack.setDefaultAnimation(R.anim.fade_in_front, R.anim.fade_out_back, R.anim.fade_in_back,
        R.anim.fade_out_front);
    if (inState != null) {
      stack.restoreState(inState.getBundle(STATE_STACK));
    }
    if (stack.size() == 0) {
      stack.replace(HiddenItemsFragment.class, FRAGMENT_HIDDEN);
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    outState.putBundle(STATE_STACK, stack.saveState());
    super.onSaveInstanceState(outState);
  }

  @Override public void onHomeClicked() {
    if (!stack.pop()) {
      finish();
    }
  }

  @Override public void onBackPressed() {
    final FragmentContract topFragment = (FragmentContract) stack.peek();
    if (topFragment != null && topFragment.onBackPressed()) {
      return;
    }

    if (stack.pop()) {
      return;
    }

    super.onBackPressed();
  }

  @Override public void onSearchClicked() {
    throw new RuntimeException("Searching from HiddenItems not supported");
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

  public static class HiddenItemsFragment
      extends ToolbarSwipeRefreshRecyclerFragment<RecyclerView.ViewHolder>
      implements HiddenItemsAdapter.ItemCallbacks {

    @Inject JobManager jobManager;

    @Inject ShowTaskScheduler showScheduler;
    @Inject MovieTaskScheduler movieScheduler;

    private HiddenViewModel viewModel;

    private HiddenItemsAdapter adapter;

    List<Show> hiddenShowsCalendar;
    List<Show> hiddenShowsWatched;
    List<Show> hiddenShowsCollected;
    List<Movie> hiddenMoviesCalendar;

    NavigationListener navigationListener;

    @Override public void onAttach(Activity activity) {
      super.onAttach(activity);
      navigationListener = (NavigationListener) activity;
    }

    @Override public void onCreate(Bundle inState) {
      super.onCreate(inState);
      AndroidSupportInjection.inject(this);

      setTitle(R.string.preference_hidden_items);
      setEmptyText(R.string.preference_hidden_empty);

      viewModel = ViewModelProviders.of(this).get(HiddenViewModel.class);
      viewModel.getShowsCalendar().observe(this, new Observer<List<Show>>() {
        @Override public void onChanged(List<Show> shows) {
          hiddenShowsCalendar = shows;
          ensureAdapter();
          adapter.updateHeaderItems(R.string.header_hidden_calendar_shows,
              new ArrayList<Object>(shows));
        }
      });
      viewModel.getShowsWatched().observe(this, new Observer<List<Show>>() {
        @Override public void onChanged(List<Show> shows) {
          hiddenShowsWatched = shows;
          ensureAdapter();
          adapter.updateHeaderItems(R.string.header_hidden_watched_shows,
              new ArrayList<Object>(shows));
        }
      });
      viewModel.getShowsCollected().observe(this, new Observer<List<Show>>() {
        @Override public void onChanged(List<Show> shows) {
          hiddenShowsCollected = shows;
          ensureAdapter();
          adapter.updateHeaderItems(R.string.header_hidden_collected_shows,
              new ArrayList<Object>(shows));
        }
      });
      viewModel.getMoviesCalendar().observe(this, new Observer<List<Movie>>() {
        @Override public void onChanged(List<Movie> movies) {
          hiddenMoviesCalendar = movies;
          ensureAdapter();
          adapter.updateHeaderItems(R.string.header_hidden_calendar_movies,
              new ArrayList<Object>(movies));
        }
      });
    }

    @Override public void onViewCreated(View view, Bundle inState) {
      super.onViewCreated(view, inState);
      getSwipeRefreshLayout().setEnabled(TraktLinkSettings.isLinked(requireContext()));
    }

    @Override public void onRefresh() {
      Job job = new SyncHiddenItems();
      job.registerOnDoneListener(onDoneListener);
      jobManager.addJob(job);
    }

    private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
      @Override public void onDone(Job job) {
        setRefreshing(false);
      }
    };

    @Override protected int getColumnCount() {
      return getResources().getInteger(R.integer.hiddenColumns);
    }

    @Override public void onShowClicked(long showId, String title, String overview) {
      navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
    }

    @Override public void displayShowInCalendar(long showId) {
      showScheduler.hideFromCalendar(showId, false);
    }

    @Override public void displayShowInWatched(long showId) {
      showScheduler.hideFromWatched(showId, false);
    }

    @Override public void displayShowInCollection(long showId) {
      showScheduler.hideFromCollected(showId, false);
    }

    @Override public void onMovieClicked(long movieId, String title, String overview) {
      navigationListener.onDisplayMovie(movieId, title, overview);
    }

    @Override public void displayMovieInCalendar(long movieId) {
      movieScheduler.hideFromCalendar(movieId, false);
    }

    private void ensureAdapter() {
      if (adapter == null) {
        adapter = new HiddenItemsAdapter(requireContext(), this);
        adapter.addHeader(R.string.header_hidden_calendar_shows);
        adapter.addHeader(R.string.header_hidden_watched_shows);
        adapter.addHeader(R.string.header_hidden_collected_shows);
        adapter.addHeader(R.string.header_hidden_calendar_movies);
        setAdapter(adapter);
      }
    }
  }
}
