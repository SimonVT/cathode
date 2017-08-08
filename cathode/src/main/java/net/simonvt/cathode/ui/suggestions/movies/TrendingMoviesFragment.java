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
package net.simonvt.cathode.ui.suggestions.movies;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.sync.movies.SyncTrendingMovies;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.SuggestionsTimestamps;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.lists.ListDialog;
import net.simonvt.cathode.ui.movies.MoviesAdapter;
import net.simonvt.cathode.ui.movies.MoviesFragment;

public class TrendingMoviesFragment extends MoviesFragment implements ListDialog.Callback {

  private enum SortBy {
    VIEWERS("viewers", Movies.SORT_VIEWERS), RATING("rating", Movies.SORT_RATING);

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
        sortBy = VIEWERS;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesFragment.sortDialog";

  private static final int LOADER_MOVIES_TRENDING = 1;

  private SortBy sortBy;

  @Override public void onCreate(Bundle inState) {
    sortBy = SortBy.fromValue(Settings.get(getContext())
        .getString(Settings.Sort.MOVIE_TRENDING, SortBy.VIEWERS.getKey()));
    super.onCreate(inState);

    setTitle(R.string.title_movies_trending);
    setEmptyText(R.string.movies_loading_trending);

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(getActivity(),
        SuggestionsTimestamps.MOVIES_TRENDING)) {
      jobManager.addJob(new SyncTrendingMovies());
      SuggestionsTimestamps.updateSuggestions(getActivity(), SuggestionsTimestamps.MOVIES_TRENDING);
    }
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_swiperefresh_recyclerview, container, false);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    Job job = new SyncTrendingMovies();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<>();
        items.add(new ListDialog.Item(R.id.sort_viewers, R.string.sort_viewers));
        items.add(new ListDialog.Item(R.id.sort_rating, R.string.sort_rating));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_viewers:
        if (sortBy != SortBy.VIEWERS) {
          sortBy = SortBy.VIEWERS;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.MOVIE_TRENDING, SortBy.VIEWERS.getKey())
              .apply();
          getLoaderManager().restartLoader(LOADER_MOVIES_TRENDING, null, this);
          scrollToTop = true;
        }
        break;

      case R.id.sort_rating:
        if (sortBy != SortBy.RATING) {
          sortBy = SortBy.RATING;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.MOVIE_TRENDING, SortBy.RATING.getKey())
              .apply();
          getLoaderManager().restartLoader(LOADER_MOVIES_TRENDING, null, this);
          scrollToTop = true;
        }
        break;
    }
  }

  protected RecyclerView.Adapter<MoviesAdapter.ViewHolder> getAdapter(Cursor cursor) {
    return new MoviesAdapter(getActivity(), this, cursor, R.layout.list_row_movie_rating,
        LibraryType.WATCHED);
  }

  @Override protected LibraryType getLibraryType() {
    return LibraryType.TRENDING;
  }

  @Override protected int getLoaderId() {
    return LOADER_MOVIES_TRENDING;
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    return new SimpleCursorLoader(getActivity(), Movies.TRENDING, null, null, null,
        sortBy.getSortOrder());
  }
}
