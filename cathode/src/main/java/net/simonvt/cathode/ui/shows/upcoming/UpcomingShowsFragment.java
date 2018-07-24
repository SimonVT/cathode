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
package net.simonvt.cathode.ui.shows.upcoming;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.android.support.AndroidSupportInjection;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.database.SimpleCursor;
import net.simonvt.cathode.common.ui.adapter.HeaderSpanLookup;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.lists.ListDialog;
import net.simonvt.cathode.ui.shows.upcoming.UpcomingSortByPreference.UpcomingSortByListener;

public class UpcomingShowsFragment
    extends ToolbarSwipeRefreshRecyclerFragment<RecyclerView.ViewHolder>
    implements UpcomingAdapter.OnRemoveListener, ListDialog.Callback, UpcomingAdapter.Callbacks {

  public static final String TAG = "net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment";

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment.sortDialog";

  @Inject JobManager jobManager;

  @Inject EpisodeTaskScheduler episodeScheduler;

  @Inject UpcomingSortByPreference upcomingSortByPreference;

  UpcomingSortBy sortBy;

  private ShowsNavigationListener navigationListener;

  @Inject UpcomingViewModelFactory viewModelFactory;
  private UpcomingViewModel viewModel;

  private int columnCount;

  private UpcomingAdapter adapter;

  boolean scrollToTop;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    sortBy = upcomingSortByPreference.get();
    upcomingSortByPreference.registerListener(upcomingSortByListener);

    setTitle(R.string.title_shows_upcoming);
    setEmptyText(R.string.empty_show_upcoming);

    columnCount = getResources().getInteger(R.integer.showsColumns);

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(UpcomingViewModel.class);
    viewModel.getShows().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        // TODO: Types!
        setCursor((SimpleCursor) cursor);
      }
    });
  }

  private UpcomingSortByListener upcomingSortByListener = new UpcomingSortByListener() {
    @Override public void onUpcomingSortByChanged(UpcomingSortBy sortBy) {
      UpcomingShowsFragment.this.sortBy = sortBy;
      scrollToTop = true;
    }
  };

  @Override public void onDestroy() {
    upcomingSortByPreference.unregisterListener(upcomingSortByListener);
    super.onDestroy();
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override protected GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
    return new HeaderSpanLookup(ensureAdapter(), columnCount);
  }

  @Override public boolean displaysMenuIcon() {
    return amITopLevel();
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_shows_upcoming);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<>();
        items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
        items.add(new ListDialog.Item(R.id.sort_next_episode, R.string.sort_next_episode));
        items.add(new ListDialog.Item(R.id.sort_last_watched, R.string.sort_last_watched));
        ListDialog.newInstance(R.string.action_sort_by, items, UpcomingShowsFragment.this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      case R.id.menu_search:
        navigationListener.onSearchClicked();
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onEpisodeClicked(long episodeId, String showTitle) {
    navigationListener.onDisplayEpisode(episodeId, showTitle);
  }

  @Override public void onCheckin(long episodeId) {
    episodeScheduler.checkin(episodeId, null, false, false, false);
  }

  @Override public void onCancelCheckin() {
    episodeScheduler.cancelCheckin();
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_title:
        if (sortBy != UpcomingSortBy.TITLE) {
          upcomingSortByPreference.set(UpcomingSortBy.TITLE);
        }
        break;

      case R.id.sort_next_episode:
        if (sortBy != UpcomingSortBy.NEXT_EPISODE) {
          upcomingSortByPreference.set(UpcomingSortBy.NEXT_EPISODE);
        }
        break;

      case R.id.sort_last_watched:
        if (sortBy != UpcomingSortBy.LAST_WATCHED) {
          upcomingSortByPreference.set(UpcomingSortBy.LAST_WATCHED);
        }
        break;
    }
  }

  @Override public void onRemove(long showId) {
    List<Cursor> cursors = ((UpcomingAdapter) getAdapter()).getCursors();
    for (Cursor cursor : cursors) {
      ((SimpleCursor) cursor).remove(showId);
    }

    ((UpcomingAdapter) getAdapter()).notifyChanged();
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    jobManager.addJob(new SyncWatching());
    Job job = new SyncWatchedShows();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  private UpcomingAdapter ensureAdapter() {
    if (adapter == null) {
      adapter = new UpcomingAdapter(getActivity(), this, this);
      adapter.addHeader(R.string.header_aired);
      adapter.addHeader(R.string.header_upcoming);
    }

    return adapter;
  }

  protected void setCursor(SimpleCursor cursor) {
    UpcomingAdapter adapter = (UpcomingAdapter) getAdapter();
    if (adapter == null) {
      adapter = ensureAdapter();
      setAdapter(adapter);
    }

    final long currentTime = System.currentTimeMillis();

    SimpleCursor airedCursor = new SimpleCursor(cursor.getColumnNames());
    SimpleCursor unairedCursor = new SimpleCursor(cursor.getColumnNames());

    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      Object[] data = cursor.get();
      final long firstAired = DataHelper.getFirstAired(cursor);
      if (firstAired <= currentTime) {
        airedCursor.add(data);
      } else {
        unairedCursor.add(data);
      }
    }

    adapter.updateCursorForHeader(R.string.header_aired, airedCursor);
    adapter.updateCursorForHeader(R.string.header_upcoming, unairedCursor);

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }
}
