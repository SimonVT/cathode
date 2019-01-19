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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.lifecycle.ViewModelProviders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.sync.movies.SyncAnticipatedMovies;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.SuggestionsTimestamps;
import net.simonvt.cathode.ui.lists.ListDialog;
import net.simonvt.cathode.ui.movies.BaseMoviesAdapter;
import net.simonvt.cathode.ui.movies.MoviesAdapter;
import net.simonvt.cathode.ui.movies.MoviesFragment;

public class AnticipatedMoviesFragment extends MoviesFragment implements ListDialog.Callback {

  enum SortBy {
    ANTICIPATED("anticipated", Movies.SORT_ANTICIPATED), TITLE("title", Movies.SORT_TITLE);

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
        sortBy = ANTICIPATED;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.suggestions.movies.AnticipatedMoviesFragment.sortDialog";

  private AnticipatedMoviesViewModel viewModel;

  private SortBy sortBy;

  @Override public void onCreate(Bundle inState) {
    sortBy = SortBy.fromValue(Settings.get(requireContext())
        .getString(Settings.Sort.MOVIE_ANTICIPATED, SortBy.ANTICIPATED.getKey()));
    super.onCreate(inState);

    setTitle(R.string.title_movies_anticipated);
    setEmptyText(R.string.movies_loading_anticipated);

    viewModel = ViewModelProviders.of(this).get(AnticipatedMoviesViewModel.class);

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(requireContext(),
        SuggestionsTimestamps.MOVIES_ANTICIPATED)) {
      jobManager.addJob(new SyncAnticipatedMovies());
      SuggestionsTimestamps.updateSuggestions(requireContext(),
          SuggestionsTimestamps.MOVIES_ANTICIPATED);
    }
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_swiperefresh_recyclerview, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    viewModel.getAnticipated().observe(this, observer);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    Job job = new SyncAnticipatedMovies();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<>();
        items.add(new ListDialog.Item(R.id.sort_anticipated, R.string.sort_anticipated));
        items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_anticipated:
        if (sortBy != SortBy.ANTICIPATED) {
          sortBy = SortBy.ANTICIPATED;
          Settings.get(requireContext())
              .edit()
              .putString(Settings.Sort.MOVIE_ANTICIPATED, SortBy.ANTICIPATED.getKey())
              .apply();
          viewModel.setSortBy(sortBy);
          scrollToTop = true;
        }
        break;

      case R.id.sort_title:
        if (sortBy != SortBy.TITLE) {
          sortBy = SortBy.TITLE;
          Settings.get(requireContext())
              .edit()
              .putString(Settings.Sort.MOVIE_ANTICIPATED, SortBy.TITLE.getKey())
              .apply();
          viewModel.setSortBy(sortBy);
          scrollToTop = true;
        }
        break;
    }
  }

  @Override protected BaseAdapter<Movie, BaseMoviesAdapter.ViewHolder> createAdapter() {
    return new MoviesAdapter(requireActivity(), this, R.layout.list_row_movie);
  }
}
