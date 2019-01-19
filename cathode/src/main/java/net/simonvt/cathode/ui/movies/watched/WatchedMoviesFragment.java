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
package net.simonvt.cathode.ui.movies.watched;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.Loader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.simonvt.cathode.R;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.database.SimpleCursor;
import net.simonvt.cathode.provider.database.SimpleCursorLoader;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.movies.SyncWatchedMovies;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.lists.ListDialog;
import net.simonvt.cathode.ui.movies.MoviesAdapter;
import net.simonvt.cathode.ui.movies.MoviesFragment;

public class WatchedMoviesFragment extends MoviesFragment implements ListDialog.Callback {

  private enum SortBy {
    TITLE("title", Movies.SORT_TITLE), WATCHED("watched", Movies.SORT_WATCHED);

    private String key;

    private String sortOrder;

    SortBy(String key, String sortOrder) {
      this.key = key;
      this.sortOrder = sortOrder;
    }

    public String getKey() {
      return key;
    }

    public String getSortOrder() {
      return sortOrder;
    }

    @Override public String toString() {
      return key;
    }

    private static final Map<String, SortBy> STRING_MAPPING = new HashMap<>();

    static {
      for (SortBy via : SortBy.values()) {
        STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
      }
    }

    public static SortBy fromValue(String value) {
      SortBy sortBy = STRING_MAPPING.get(value.toUpperCase(Locale.US));
      if (sortBy == null) {
        sortBy = TITLE;
      }
      return sortBy;
    }
  }

  public static final String TAG = "net.simonvt.cathode.ui.movies.watched.WatchedMoviesFragment";

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.movies.watched.WatchedMoviesFragment.sortDialog";

  private static final int LOADER_MOVIES_WATCHED = 1;

  private SortBy sortBy;

  @Override public void onCreate(Bundle inState) {
    sortBy = SortBy.fromValue(
        Settings.get(getContext()).getString(Settings.Sort.MOVIE_WATCHED, SortBy.TITLE.getKey()));

    super.onCreate(inState);

    setEmptyText(R.string.empty_movie_watched);
    setTitle(R.string.title_movies_watched);
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_movies_watched);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    jobManager.addJob(new SyncWatching());
    Job job = new SyncWatchedMovies();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_sort) {
      ArrayList<ListDialog.Item> items = new ArrayList<>();
      items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
      items.add(new ListDialog.Item(R.id.sort_watched, R.string.sort_watched));
      ListDialog.newInstance(R.string.action_sort_by, items, this)
          .show(getFragmentManager(), DIALOG_SORT);
      return true;
    }

    return super.onMenuItemClick(item);
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_title:
        if (sortBy != SortBy.TITLE) {
          sortBy = SortBy.TITLE;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.MOVIE_WATCHED, SortBy.TITLE.getKey())
              .apply();
          getLoaderManager().restartLoader(getLoaderId(), null, this);
          scrollToTop = true;
        }
        break;

      case R.id.sort_watched:
        if (sortBy != SortBy.WATCHED) {
          sortBy = SortBy.WATCHED;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.MOVIE_WATCHED, SortBy.WATCHED.getKey())
              .apply();
          getLoaderManager().restartLoader(getLoaderId(), null, this);
          scrollToTop = true;
        }
        break;
    }
  }

  @Override protected int getLoaderId() {
    return LOADER_MOVIES_WATCHED;
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    return new SimpleCursorLoader(getActivity(), Movies.MOVIES_WATCHED, MoviesAdapter.PROJECTION,
        null, null, sortBy.getSortOrder());
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    data.moveToPosition(-1);
    super.onLoadFinished(loader, data);
  }
}
