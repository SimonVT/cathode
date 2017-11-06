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
package net.simonvt.cathode.ui.show;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import dagger.android.support.AndroidSupportInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows;
import net.simonvt.cathode.provider.database.SimpleCursor;
import net.simonvt.cathode.provider.database.SimpleCursorLoader;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter;

public class RelatedShowsFragment
    extends ToolbarSwipeRefreshRecyclerFragment<ShowDescriptionAdapter.ViewHolder>
    implements ShowDescriptionAdapter.ShowCallbacks {

  private static final String TAG = "net.simonvt.cathode.ui.show.RelatedShowsFragment";

  private static final String ARG_SHOW_ID =
      "net.simonvt.cathode.ui.show.RelatedShowsFragment.showId";

  private static final int LOADER_SHOWS_RELATED = 1;

  @Inject JobManager jobManager;
  @Inject ShowTaskScheduler showScheduler;

  private ShowsNavigationListener navigationListener;

  private long showId;

  private ShowDescriptionAdapter showsAdapter;

  public static String getTag(long showId) {
    return TAG + "/" + showId + "/" + Ids.newId();
  }

  public static Bundle getArgs(long showId) {
    Preconditions.checkArgument(showId >= 0, "showId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_SHOW_ID, showId);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    showId = getArguments().getLong(ARG_SHOW_ID);

    setEmptyText(R.string.empty_show_related);
    setTitle(R.string.title_related);

    getLoaderManager().initLoader(LOADER_SHOWS_RELATED, null, relatedLoader);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    showScheduler.syncRelated(showId, onDoneListener);
  }

  @Override public void onShowClick(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
  }

  @Override public void setIsInWatchlist(long showId, boolean inWatchlist) {
    showScheduler.setIsInWatchlist(showId, inWatchlist);
  }

  private void setCursor(Cursor cursor) {
    if (showsAdapter == null) {
      showsAdapter = new ShowDescriptionAdapter(getActivity(), this, cursor, false);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);
  }

  private LoaderManager.LoaderCallbacks<SimpleCursor> relatedLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), RelatedShows.fromShow(showId),
              ShowDescriptionAdapter.PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          setCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
