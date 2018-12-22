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
package net.simonvt.cathode.ui.shows.watchlist;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.ui.adapter.HeaderSpanLookup;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeWatchlist;
import net.simonvt.cathode.remote.sync.shows.SyncShowsWatchlist;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;

public class ShowsWatchlistFragment
    extends ToolbarSwipeRefreshRecyclerFragment<RecyclerView.ViewHolder>
    implements ShowWatchlistAdapter.RemoveListener, ShowWatchlistAdapter.ItemCallbacks {

  public static final String TAG = "net.simonvt.cathode.ui.shows.watchlist.ShowsWatchlistFragment";

  @Inject JobManager jobManager;

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  private ShowsNavigationListener navigationListener;

  private ShowsWatchlistViewModel viewModel;

  private int columnCount;

  private ShowWatchlistAdapter adapter;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    columnCount = getResources().getInteger(R.integer.showsColumns);

    setEmptyText(R.string.empty_show_watchlist);
    setTitle(R.string.title_shows_watchlist);

    viewModel = ViewModelProviders.of(this).get(ShowsWatchlistViewModel.class);
    viewModel.getShows().observe(this, new Observer<List<Show>>() {
      @Override public void onChanged(List<Show> shows) {
        setShows(shows);
      }
    });
    viewModel.getEpisodes().observe(this, new Observer<List<Episode>>() {
      @Override public void onChanged(List<Episode> episodes) {
        setEpisodes(episodes);
      }
    });
  }

  @Override public boolean displaysMenuIcon() {
    return amITopLevel();
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override protected GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
    return new HeaderSpanLookup(ensureAdapter(), columnCount);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    getToolbar().setNavigationOnClickListener(navigationClickListener);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    Job job = new SyncEpisodeWatchlist();
    jobManager.addJob(job);
    job = new SyncShowsWatchlist();
    // Jobs are executed in order, so only attach listener to the last one
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  private View.OnClickListener navigationClickListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      navigationListener.onHomeClicked();
    }
  };

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_shows);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onSearchClicked();
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onShowClicked(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
  }

  @Override public void onRemoveShowFromWatchlist(long showId) {
    showScheduler.setIsInWatchlist(showId, false);
  }

  @Override public void onEpisodeClicked(long episodeId, String showTitle) {
    navigationListener.onDisplayEpisode(episodeId, showTitle);
  }

  @Override public void onRemoveEpisodeFromWatchlist(long episodeId) {
    episodeScheduler.setIsInWatchlist(episodeId, false);
  }

  @Override public void onRemoveItem(Object item) {
    adapter.removeItem(item);
  }

  private ShowWatchlistAdapter ensureAdapter() {
    if (adapter == null) {
      adapter = new ShowWatchlistAdapter(getActivity(), this, this);
      adapter.addHeader(R.string.header_shows);
      adapter.addHeader(R.string.header_episodes);
    }

    return adapter;
  }

  private void setShows(List<Show> shows) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter());
    }

    List<Object> items = new ArrayList<>(shows);
    ((ShowWatchlistAdapter) getAdapter()).updateHeaderItems(R.string.header_shows, items);
  }

  private void setEpisodes(List<Episode> episodes) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter());
    }

    List<Object> items = new ArrayList<>(episodes);
    ((ShowWatchlistAdapter) getAdapter()).updateHeaderItems(R.string.header_episodes, items);
  }
}
