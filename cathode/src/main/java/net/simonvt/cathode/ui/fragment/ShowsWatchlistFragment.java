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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.MutableCursor;
import net.simonvt.cathode.database.MutableCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.HeaderSpanLookup;
import net.simonvt.cathode.ui.adapter.ShowWatchlistAdapter;

public class ShowsWatchlistFragment extends GridRecyclerViewFragment<RecyclerView.ViewHolder>
    implements ShowWatchlistAdapter.RemoveListener, ShowWatchlistAdapter.OnItemClickListener {

  @Inject TraktTaskQueue queue;

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

    setHasOptionsMenu(true);

    getLoaderManager().initLoader(HomeActivity.LOADER_SHOWS_WATCHLIST, null, showsCallback);
    getLoaderManager().initLoader(HomeActivity.LOADER_EPISODES_WATCHLIST, null, episodeCallback);

    columnCount = getResources().getInteger(R.integer.showsColumns);

    setEmptyText(R.string.empty_show_watchlist);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_watchlist);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override protected GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
    return new HeaderSpanLookup(ensureAdapter(), columnCount);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_shows, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        queue.add(new SyncTask());
        return true;

      case R.id.menu_search:
        navigationListener.onStartShowSearch();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onShowClicked(int position, long id) {
    Cursor c = ((ShowWatchlistAdapter) getAdapter()).getCursor(position);
    navigationListener.onDisplayShow(id, c.getString(c.getColumnIndex(ShowColumns.TITLE)),
        LibraryType.WATCHED);
  }

  @Override public void onEpisodeClicked(int position, long id) {
    Cursor c = ((ShowWatchlistAdapter) getAdapter()).getCursor(position);
    navigationListener.onDisplayEpisode(id, c.getString(c.getColumnIndex(EpisodeColumns.TITLE)));
  }

  private void throttleLoaders() {
    Loader l = getLoaderManager().getLoader(BaseActivity.LOADER_EPISODES_WATCHLIST);
    MutableCursorLoader loader = (MutableCursorLoader) l;
    loader.throttle(2000);

    l = getLoaderManager().getLoader(BaseActivity.LOADER_SHOWS_WATCHLIST);
    loader = (MutableCursorLoader) l;
    loader.throttle(2000);
  }

  @Override public void onRemoveItem(View view, int position) {
    throttleLoaders();
    ShowWatchlistAdapter adapter = (ShowWatchlistAdapter) getAdapter();
    MutableCursor cursor =
        (MutableCursor) (((ShowWatchlistAdapter) getAdapter()).getCursor(position));
    final int correctedPosition = adapter.getCursorPosition(position);
    cursor.remove(correctedPosition);
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

    throttleLoaders();
    ((ShowWatchlistAdapter) getAdapter()).updateCursorForHeader(R.string.header_shows, cursor);
  }

  private void setEpisodeCursor(Cursor cursor) {
    if (getAdapter() == null) {
      setAdapter(ensureAdapter());
    }

    throttleLoaders();
    ((ShowWatchlistAdapter) getAdapter()).updateCursorForHeader(R.string.header_episodes, cursor);
  }

  private LoaderManager.LoaderCallbacks<MutableCursor> showsCallback =
      new LoaderManager.LoaderCallbacks<MutableCursor>() {
        @Override public Loader<MutableCursor> onCreateLoader(int id, Bundle args) {
          final Uri contentUri = Shows.SHOWS_WATCHLIST;
          MutableCursorLoader loader = new MutableCursorLoader(getActivity(), contentUri,
              ShowWatchlistAdapter.PROJECTION_SHOW, null, null, Shows.DEFAULT_SORT);
          loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
          return loader;
        }

        @Override public void onLoadFinished(Loader<MutableCursor> loader, MutableCursor data) {
          setShowCursor(data);
        }

        @Override public void onLoaderReset(Loader<MutableCursor> loader) {
        }
      };

  private LoaderManager.LoaderCallbacks<MutableCursor> episodeCallback =
      new LoaderManager.LoaderCallbacks<MutableCursor>() {
        @Override public Loader<MutableCursor> onCreateLoader(int id, Bundle args) {
          MutableCursorLoader loader =
              new MutableCursorLoader(getActivity(), Episodes.EPISODES_IN_WATCHLIST,
                  ShowWatchlistAdapter.PROJECTION_EPISODE, null, null,
                  EpisodeColumns.SHOW_ID + " ASC");
          loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
          return loader;
        }

        @Override public void onLoadFinished(Loader<MutableCursor> loader, MutableCursor data) {
          setEpisodeCursor(data);
        }

        @Override public void onLoaderReset(Loader<MutableCursor> loader) {
        }
      };
}
