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
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.MutableCursor;
import net.simonvt.cathode.database.MutableCursorLoader;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.ShowWatchlistAdapter;
import net.simonvt.cathode.widget.AnimatorHelper;
import net.simonvt.cathode.widget.StaggeredGridAnimator;
import net.simonvt.cathode.widget.StaggeredGridView;

public class ShowsWatchlistFragment extends StaggeredGridFragment
    implements ShowWatchlistAdapter.RemoveListener {

  @Inject TraktTaskQueue queue;

  private ShowsNavigationListener navigationListener;

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
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_watchlist);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_shows_watchlist, container, false);
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

  @Override protected void onItemClick(StaggeredGridView parent, View v, int position, long id) {
    if (((ShowWatchlistAdapter) getAdapter()).isShow(position)) {
      Cursor c = (Cursor) getAdapter().getItem(position);
      navigationListener.onDisplayShow(id,
          c.getString(c.getColumnIndex(CathodeContract.Shows.TITLE)), LibraryType.WATCHED);
    } else {
      Cursor c = (Cursor) getAdapter().getItem(position);
      navigationListener.onDisplayEpisode(id,
          c.getString(c.getColumnIndex(CathodeContract.Shows.TITLE)));
    }
  }

  @Override public void onRemoveItem(View view, int position) {
    AnimatorHelper.removeView(getGridView(), view, animatorCallback);
  }

  private AnimatorHelper.Callback animatorCallback = new AnimatorHelper.Callback() {
    @Override public void removeItem(int position) {
      ShowWatchlistAdapter adapter = (ShowWatchlistAdapter) getAdapter();
      MutableCursor cursor = (MutableCursor) adapter.getItem(position);
      final int correctedPosition = adapter.getCorrectedPosition(position);
      cursor.remove(correctedPosition);
    }

    @Override public void onAnimationEnd() {
    }
  };

  private void ensureAdapter() {
    if (getAdapter() == null) {
      setAdapter(new ShowWatchlistAdapter(getActivity(), this));
    }
  }

  private void setShowCursor(Cursor cursor) {
    ensureAdapter();
    Loader l = getLoaderManager().getLoader(BaseActivity.LOADER_EPISODES_WATCHLIST);
    MutableCursorLoader loader = (MutableCursorLoader) l;
    loader.throttle(2000);
    StaggeredGridAnimator animator = new StaggeredGridAnimator(getGridView());
    ((ShowWatchlistAdapter) getAdapter()).changeShowCursor(cursor);
    animator.animate();
  }

  private void setEpisodeCursor(Cursor cursor) {
    ensureAdapter();
    Loader l = getLoaderManager().getLoader(BaseActivity.LOADER_SHOWS_WATCHLIST);
    MutableCursorLoader loader = (MutableCursorLoader) l;
    loader.throttle(2000);
    StaggeredGridAnimator animator = new StaggeredGridAnimator(getGridView());
    ((ShowWatchlistAdapter) getAdapter()).changeEpisodeCursor(cursor);
    animator.animate();
  }

  private LoaderManager.LoaderCallbacks<MutableCursor> showsCallback =
      new LoaderManager.LoaderCallbacks<MutableCursor>() {
        @Override public Loader<MutableCursor> onCreateLoader(int id, Bundle args) {
          final Uri contentUri = CathodeContract.Shows.SHOWS_WATCHLIST;
          MutableCursorLoader loader = new MutableCursorLoader(getActivity(), contentUri,
              ShowWatchlistAdapter.PROJECTION_SHOW, null, null, CathodeContract.Shows.DEFAULT_SORT);
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
              new MutableCursorLoader(getActivity(), CathodeContract.Episodes.WATCHLIST_URI,
                  ShowWatchlistAdapter.PROJECTION_EPISODE, null, null,
                  CathodeContract.Episodes.SHOW_ID + " ASC");
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
