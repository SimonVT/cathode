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
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.remote.PriorityQueue;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.MoviesAdapter;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;

public abstract class MoviesFragment extends GridRecyclerViewFragment<MoviesAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<Cursor>, MoviesAdapter.MovieClickListener {

  @Inject TraktTaskQueue queue;

  @Inject @PriorityQueue TraktTaskQueue priorityQueue;

  private MoviesNavigationListener navigationListener;

  private Cursor cursor;

  private int columnCount;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (MoviesNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement MoviesNavigationListener");
    }
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);
    setHasOptionsMenu(true);

    getLoaderManager().initLoader(getLoaderId(), null, this);

    columnCount = getResources().getInteger(R.integer.movieColumns);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.fragment_movies, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        queue.add(new SyncTask());
        return true;

      case R.id.menu_search:
        navigationListener.onStartMovieSearch();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void onMovieClicked(View v, int position, long id) {
    navigationListener.onDisplayMovie(id,
        cursor.getString(cursor.getColumnIndex(MovieColumns.TITLE)));
  }

  protected RecyclerView.Adapter<MoviesAdapter.ViewHolder> getAdapter(Cursor cursor) {
    return new MoviesAdapter(getActivity(), this, cursor);
  }

  void setCursor(Cursor cursor) {
    this.cursor = cursor;
    if (getAdapter() == null) {
      setAdapter(getAdapter(cursor));
    } else {
      ((RecyclerCursorAdapter) getAdapter()).changeCursor(cursor);
    }
  }

  protected abstract int getLoaderId();

  @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<Cursor> loader) {
    setAdapter(null);
  }
}
