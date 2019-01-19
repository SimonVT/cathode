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
package net.simonvt.cathode.ui.shows;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import dagger.android.support.AndroidSupportInjection;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.ShowWithEpisode;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;

public abstract class ShowsFragment
    extends ToolbarSwipeRefreshRecyclerFragment<ShowsWithNextAdapter.ViewHolder>
    implements ShowsWithNextAdapter.Callbacks {

  @Inject protected JobManager jobManager;

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  protected BaseAdapter<ShowWithEpisode, ShowsWithNextAdapter.ViewHolder> showsAdapter;

  private ShowsNavigationListener navigationListener;

  private int columnCount;

  protected boolean scrollToTop;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    columnCount = getResources().getInteger(R.integer.showsColumns);
  }

  @Override public boolean displaysMenuIcon() {
    return amITopLevel();
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

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

  @Override public void onShowClick(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, getLibraryType());
  }

  @Override public void onRemoveFromWatchlist(long showId) {
    showScheduler.setIsInWatchlist(showId, false);
  }

  @Override public void onCheckin(long episodeId) {
    episodeScheduler.checkin(episodeId, null, false, false, false);
  }

  @Override public void onCancelCheckin() {
    episodeScheduler.cancelCheckin();
  }

  @Override public void onCollectNext(long showId) {
    showScheduler.collectedNext(showId);
  }

  @Override public void onHideFromWatched(long showId) {
    showScheduler.hideFromWatched(showId, true);
  }

  @Override public void onHideFromCollection(long showId) {
    showScheduler.hideFromCollected(showId, true);
  }

  protected ShowsWithNextAdapter createAdapter() {
    return new ShowsWithNextAdapter(requireActivity(), this, getLibraryType());
  }

  protected Observer<List<ShowWithEpisode>> observer =
      new Observer<List<ShowWithEpisode>>() {
        @Override public void onChanged(List<ShowWithEpisode> shows) {
          setShows(shows);
        }
      };

  protected void setShows(List<ShowWithEpisode> shows) {
    if (showsAdapter == null) {
      showsAdapter = createAdapter();
      setAdapter(showsAdapter);
    }

    showsAdapter.setList(shows);

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }

  protected abstract LibraryType getLibraryType();
}
