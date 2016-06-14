/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeWatchlist;
import net.simonvt.cathode.remote.sync.shows.SyncShowsWatchlist;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.HeaderSpanLookup;
import net.simonvt.cathode.ui.adapter.ShowWatchlistAdapter;
import net.simonvt.schematic.Cursors;

public class ShowsWatchlistFragment
    extends ToolbarSwipeRefreshRecyclerFragment<RecyclerView.ViewHolder>
    implements ShowWatchlistAdapter.RemoveListener, ShowWatchlistAdapter.OnItemClickListener {

  @Inject JobManager jobManager;

  private ShowsNavigationListener navigationListener;

  private int columnCount;

  private ShowWatchlistAdapter adapter;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (ShowsNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement ShowsNavigationListener");
    }
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    getLoaderManager().initLoader(Loaders.SHOWS_WATCHLIST, null, showsCallback);
    getLoaderManager().initLoader(Loaders.EPISODES_WATCHLIST, null, episodeCallback);

    columnCount = getResources().getInteger(R.integer.showsColumns);

    setEmptyText(R.string.empty_show_watchlist);
    setTitle(R.string.title_shows_watchlist);
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
    toolbar.setNavigationOnClickListener(navigationClickListener);
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
        navigationListener.onSearchShow();
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onShowClicked(int position, long id) {
    Cursor c = ((ShowWatchlistAdapter) getAdapter()).getCursor(position);
    final String title = Cursors.getString(c, ShowColumns.TITLE);
    final String overview = Cursors.getString(c, ShowColumns.OVERVIEW);
    navigationListener.onDisplayShow(id, title, overview, LibraryType.WATCHED);
  }

  @Override public void onEpisodeClicked(int position, long id) {
    Cursor c = ((ShowWatchlistAdapter) getAdapter()).getCursor(position);
    navigationListener.onDisplayEpisode(id, Cursors.getString(c, ShowColumns.TITLE));
  }

  private void throttleLoaders() {
    Loader l = getLoaderManager().getLoader(Loaders.EPISODES_WATCHLIST);
    if (l != null) {
      SimpleCursorLoader loader = (SimpleCursorLoader) l;
      loader.throttle(SimpleCursorLoader.DEFAULT_THROTTLE);
    }

    l = getLoaderManager().getLoader(Loaders.SHOWS_WATCHLIST);
    if (l != null) {
      SimpleCursorLoader loader = (SimpleCursorLoader) l;
      loader.throttle(SimpleCursorLoader.DEFAULT_THROTTLE);
    }
  }

  @Override public void onRemoveItem(View view, int position, long id) {
    throttleLoaders();
    SimpleCursor cursor =
        (SimpleCursor) (((ShowWatchlistAdapter) getAdapter()).getCursor(position));
    cursor.remove(id);
    ((ShowWatchlistAdapter) getAdapter()).notifyChanged();
  }

  private ShowWatchlistAdapter ensureAdapter() {
    if (adapter == null) {
      adapter = new ShowWatchlistAdapter(getActivity(), this, this);
      adapter.addHeader(R.string.header_shows);
      adapter.addHeader(R.string.header_episodes);
    }

    return adapter;
  }

  private void setShowCursor(Cursor cursor) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter());
    }

    //throttleLoaders();
    ((ShowWatchlistAdapter) getAdapter()).updateCursorForHeader(R.string.header_shows, cursor);
  }

  private void setEpisodeCursor(Cursor cursor) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter());
    }

    //throttleLoaders();
    ((ShowWatchlistAdapter) getAdapter()).updateCursorForHeader(R.string.header_episodes, cursor);
  }

  private LoaderManager.LoaderCallbacks<SimpleCursor> showsCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Shows.SHOWS_WATCHLIST,
              ShowWatchlistAdapter.PROJECTION_SHOW, null, null, Shows.DEFAULT_SORT);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          setShowCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleCursor> episodeCallback =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Episodes.EPISODES_IN_WATCHLIST,
              ShowWatchlistAdapter.PROJECTION_EPISODE,
              Tables.EPISODES + "." + EpisodeColumns.NEEDS_SYNC + "=0", null,
              EpisodeColumns.SHOW_ID + " ASC");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          setEpisodeCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
