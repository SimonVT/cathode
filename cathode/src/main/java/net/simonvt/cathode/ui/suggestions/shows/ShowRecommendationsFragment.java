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
package net.simonvt.cathode.ui.suggestions.shows;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import dagger.android.support.AndroidSupportInjection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.database.SimpleCursor;
import net.simonvt.cathode.common.ui.fragment.SwipeRefreshRecyclerFragment;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.sync.shows.SyncShowRecommendations;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.SuggestionsTimestamps;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.lists.ListDialog;
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter;

public class ShowRecommendationsFragment
    extends SwipeRefreshRecyclerFragment<ShowDescriptionAdapter.ViewHolder>
    implements ShowRecommendationsAdapter.DismissListener, ListDialog.Callback,
    ShowDescriptionAdapter.ShowCallbacks {

  enum SortBy {
    RELEVANCE("relevance", Shows.SORT_RECOMMENDED), RATING("rating", Shows.SORT_RATING);

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
        sortBy = RELEVANCE;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.suggestions.shows.ShowRecommendationsFragment.sortDialog";

  private ShowRecommendationsViewModel viewModel;
  private ShowRecommendationsAdapter showsAdapter;

  private ShowsNavigationListener navigationListener;

  @Inject JobManager jobManager;

  @Inject ShowTaskScheduler showScheduler;

  private SimpleCursor cursor;

  private SortBy sortBy;

  private int columnCount;

  private boolean scrollToTop;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    sortBy = SortBy.fromValue(Settings.get(getContext())
        .getString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RELEVANCE.getKey()));

    columnCount = getResources().getInteger(R.integer.showsColumns);

    setTitle(R.string.title_shows_recommended);
    setEmptyText(R.string.recommendations_empty);

    viewModel = ViewModelProviders.of(this).get(ShowRecommendationsViewModel.class);
    viewModel.getRecommendations().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        setCursor(cursor);
      }
    });

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(getActivity(),
        SuggestionsTimestamps.SHOWS_RECOMMENDED)) {
      jobManager.addJob(new SyncShowRecommendations());
      SuggestionsTimestamps.updateSuggestions(getActivity(),
          SuggestionsTimestamps.SHOWS_RECOMMENDED);
    }
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    Job job = new SyncShowRecommendations();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<>();
        items.add(new ListDialog.Item(R.id.sort_relevance, R.string.sort_relevance));
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
      case R.id.sort_relevance:
        if (sortBy != SortBy.RELEVANCE) {
          sortBy = SortBy.RELEVANCE;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RELEVANCE.getKey())
              .apply();
          viewModel.setSortBy(sortBy);
          scrollToTop = true;
        }
        break;

      case R.id.sort_rating:
        if (sortBy != SortBy.RATING) {
          sortBy = SortBy.RATING;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RATING.getKey())
              .apply();
          viewModel.setSortBy(sortBy);
          scrollToTop = true;
        }
        break;
    }
  }

  @Override public void onShowClick(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
  }

  @Override public void setIsInWatchlist(long showId, boolean inWatchlist) {
    showScheduler.setIsInWatchlist(showId, inWatchlist);
  }

  @Override public void onDismissItem(final View view, final long id) {
    showScheduler.dismissRecommendation(id);

    if (cursor != null) {
      cursor.remove(id);
      showsAdapter.notifyChanged();
    }
  }

  private void setCursor(Cursor c) {
    cursor = (SimpleCursor) c;

    if (showsAdapter == null) {
      showsAdapter = new ShowRecommendationsAdapter(getActivity(), this, cursor, this);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }
}
