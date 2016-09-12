/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.HiddenColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.ui.adapter.HiddenItemsAdapter;
import net.simonvt.cathode.ui.fragment.ActorsFragment;
import net.simonvt.cathode.ui.fragment.CommentFragment;
import net.simonvt.cathode.ui.fragment.CommentsFragment;
import net.simonvt.cathode.ui.fragment.EpisodeFragment;
import net.simonvt.cathode.ui.fragment.MovieFragment;
import net.simonvt.cathode.ui.fragment.SeasonFragment;
import net.simonvt.cathode.ui.fragment.ShowFragment;
import net.simonvt.cathode.ui.fragment.ToolbarGridFragment;
import net.simonvt.cathode.ui.lists.ListFragment;
import net.simonvt.cathode.util.FragmentStack;

public class HiddenItems extends BaseActivity
    implements NavigationClickListener, NavigationListener {

  private static final String FRAGMENT_HIDDEN =
      "net.simonvt.cathode.ui.HiddenItems.HiddenItemsFragment";

  private static final String STATE_STACK = "net.simonvt.cathode.ui.HomeActivity.stack";

  private static final int LOADER_SHOWS_CALENDAR = 1;
  private static final int LOADER_SHOWS_WATCHED = 2;
  private static final int LOADER_SHOWS_COLLECTED = 3;
  private static final int LOADER_MOVIES_CALENDAR = 4;
  private static final int LOADER_MOVIES_WATCHED = 5;
  private static final int LOADER_MOVIES_COLLECTED = 6;

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

  @Override
  public void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    stack.push(SeasonFragment.class, SeasonFragment.TAG,
        SeasonFragment.getArgs(showId, seasonId, showTitle, seasonNumber, type));
  }

  @Override public void onDisplayShowActors(long showId, String title) {
    stack.push(ActorsFragment.class, ActorsFragment.TAG, ActorsFragment.forShow(showId, title));
  }

  @Override public void onDisplayRelatedShows(long showId, String title) {
    throw new RuntimeException("Not implemented");
  }

  @Override public void onDisplayMovie(long movieId, String title, String overview) {
    stack.push(MovieFragment.class, MovieFragment.getTag(movieId),
        MovieFragment.getArgs(movieId, title, overview));
  }

  @Override public void onDisplayMovieActors(long movieId, String title) {
    stack.push(ActorsFragment.class, ActorsFragment.TAG, ActorsFragment.forMovie(movieId, title));
  }

  @Override public void onDisplayRelatedMovies(long movieId, String title) {
    throw new RuntimeException("Not implemented");
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

  @Override public void displayFragment(Class clazz, String tag) {
    stack.push(clazz, tag, null);
  }

  @Override public boolean isFragmentTopLevel(Fragment fragment) {
    return stack.positionInstack(fragment) == 0;
  }

  public static class HiddenItemsFragment extends ToolbarGridFragment<RecyclerView.ViewHolder>
      implements HiddenItemsAdapter.OnItemClickListener, HiddenItemsAdapter.RemoveListener {

    private HiddenItemsAdapter adapter;

    Cursor hiddenShowsCalendar;
    Cursor hiddenShowsWatched;
    Cursor hiddenShowsCollected;
    Cursor hiddenMoviesCalendar;
    Cursor hiddenMoviesWatched;
    Cursor hiddenMoviesCollected;

    NavigationListener navigationListener;

    @Override public void onAttach(Activity activity) {
      super.onAttach(activity);
      navigationListener = (NavigationListener) activity;
    }

    @Override public void onCreate(Bundle inState) {
      super.onCreate(inState);
      setTitle(R.string.preference_hidden_items);
      setEmptyText(R.string.preference_hidden_empty);

      getLoaderManager().initLoader(LOADER_SHOWS_CALENDAR, null, hiddenShowsCalendarCallback);
      getLoaderManager().initLoader(LOADER_SHOWS_WATCHED, null, hiddenShowsWatchedCallback);
      getLoaderManager().initLoader(LOADER_SHOWS_COLLECTED, null, hiddenShowsCollectedCallback);
      getLoaderManager().initLoader(LOADER_MOVIES_CALENDAR, null, hiddenMoviesCalendarCallback);
      getLoaderManager().initLoader(LOADER_MOVIES_WATCHED, null, hiddenMoviesWatchedCallback);
      getLoaderManager().initLoader(LOADER_MOVIES_COLLECTED, null, hiddenMoviesCollectedCallback);
    }

    @Override protected int getColumnCount() {
      return getResources().getInteger(R.integer.hiddenColumns);
    }

    @Override public void onShowClicked(int position, long showId, String title, String overview) {
      navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
    }

    @Override
    public void onMovieClicked(int position, long movieId, String title, String overview) {
      navigationListener.onDisplayMovie(movieId, title, overview);
    }

    @Override public void onRemoveItem(View view, int position, long id) {

    }

    private void ensureAdapter() {
      if (adapter == null) {
        adapter = new HiddenItemsAdapter(getActivity(), this, this);
        adapter.addHeader(R.string.header_hidden_calendar_shows);
        adapter.addHeader(R.string.header_hidden_watched_shows);
        adapter.addHeader(R.string.header_hidden_collected_shows);
        adapter.addHeader(R.string.header_hidden_calendar_movies);
        adapter.addHeader(R.string.header_hidden_watched_movies);
        adapter.addHeader(R.string.header_hidden_collected_movies);
        setAdapter(adapter);
      }
    }

    private LoaderManager.LoaderCallbacks<SimpleCursor> hiddenShowsCalendarCallback =
        new LoaderManager.LoaderCallbacks<SimpleCursor>() {
          @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
            return new SimpleCursorLoader(getActivity(), Shows.SHOWS,
                HiddenItemsAdapter.PROJECTION_SHOW, HiddenColumns.HIDDEN_CALENDAR + "=1", null,
                Shows.SORT_TITLE);
          }

          @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
            ensureAdapter();
            adapter.updateCursorForHeader(R.string.header_hidden_calendar_shows, data);
            hiddenShowsCalendar = data;
          }

          @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
          }
        };

    private LoaderManager.LoaderCallbacks<SimpleCursor> hiddenShowsWatchedCallback =
        new LoaderManager.LoaderCallbacks<SimpleCursor>() {
          @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
            return new SimpleCursorLoader(getActivity(), Shows.SHOWS,
                HiddenItemsAdapter.PROJECTION_SHOW, HiddenColumns.HIDDEN_WATCHED + "=1", null,
                Shows.SORT_TITLE);
          }

          @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
            ensureAdapter();
            adapter.updateCursorForHeader(R.string.header_hidden_watched_shows, data);
            hiddenShowsWatched = data;
          }

          @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
          }
        };

    private LoaderManager.LoaderCallbacks<SimpleCursor> hiddenShowsCollectedCallback =
        new LoaderManager.LoaderCallbacks<SimpleCursor>() {
          @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
            return new SimpleCursorLoader(getActivity(), Shows.SHOWS,
                HiddenItemsAdapter.PROJECTION_SHOW, HiddenColumns.HIDDEN_COLLECTED + "=1", null,
                Shows.SORT_TITLE);
          }

          @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
            ensureAdapter();
            adapter.updateCursorForHeader(R.string.header_hidden_collected_shows, data);
            hiddenShowsCollected = data;
          }

          @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
          }
        };

    private LoaderManager.LoaderCallbacks<SimpleCursor> hiddenMoviesCalendarCallback =
        new LoaderManager.LoaderCallbacks<SimpleCursor>() {
          @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
            return new SimpleCursorLoader(getActivity(), Movies.MOVIES,
                HiddenItemsAdapter.PROJECTION_MOVIES, HiddenColumns.HIDDEN_CALENDAR + "=1", null,
                Movies.SORT_TITLE);
          }

          @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
            ensureAdapter();
            adapter.updateCursorForHeader(R.string.header_hidden_calendar_movies, data);
            hiddenMoviesCalendar = data;
          }

          @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
          }
        };

    private LoaderManager.LoaderCallbacks<SimpleCursor> hiddenMoviesWatchedCallback =
        new LoaderManager.LoaderCallbacks<SimpleCursor>() {
          @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
            return new SimpleCursorLoader(getActivity(), Movies.MOVIES,
                HiddenItemsAdapter.PROJECTION_MOVIES, HiddenColumns.HIDDEN_WATCHED + "=1", null,
                Movies.SORT_TITLE);
          }

          @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
            ensureAdapter();
            adapter.updateCursorForHeader(R.string.header_hidden_watched_movies, data);
            hiddenMoviesWatched = data;
          }

          @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
          }
        };

    private LoaderManager.LoaderCallbacks<SimpleCursor> hiddenMoviesCollectedCallback =
        new LoaderManager.LoaderCallbacks<SimpleCursor>() {
          @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
            return new SimpleCursorLoader(getActivity(), Movies.MOVIES,
                HiddenItemsAdapter.PROJECTION_MOVIES, HiddenColumns.HIDDEN_COLLECTED + "=1", null,
                Movies.SORT_TITLE);
          }

          @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
            ensureAdapter();
            adapter.updateCursorForHeader(R.string.header_hidden_collected_movies, data);
            hiddenMoviesCollected = data;
          }

          @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
          }
        };
  }
}
