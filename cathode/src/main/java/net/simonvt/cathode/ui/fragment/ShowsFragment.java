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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.ui.adapter.ShowClickListener;
import net.simonvt.cathode.ui.adapter.ShowsWithNextAdapter;

public abstract class ShowsFragment<D extends Cursor>
    extends GridRecyclerViewFragment<ShowsWithNextAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<D>, ShowClickListener {

  @Inject TraktTaskQueue queue;

  protected RecyclerCursorAdapter showsAdapter;

  private ShowsNavigationListener navigationListener;

  private Cursor cursor;

  private int columnCount;

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

    getLoaderManager().initLoader(getLoaderId(), null, this);

    columnCount = getResources().getInteger(R.integer.showsColumns);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
  }

  @Override public void onDetach() {
    super.onDetach();
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
        return false;
    }
  }

  @Override public void onShowClick(View view, int position, long id) {
    navigationListener.onDisplayShow(id, cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE)),
        getLibraryType());
  }

  protected ShowsWithNextAdapter getAdapter(Cursor cursor) {
    return new ShowsWithNextAdapter(getActivity(), this, cursor, getLibraryType());
  }

  protected void setCursor(Cursor cursor) {
    this.cursor = cursor;

    if (showsAdapter == null) {
      showsAdapter = getAdapter(cursor);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);
  }

  protected abstract LibraryType getLibraryType();

  protected abstract int getLoaderId();

  @Override public void onLoadFinished(Loader<D> cursorLoader, D cursor) {
    setCursor(cursor);
  }

  @Override public void onLoaderReset(Loader<D> cursorLoader) {
    showsAdapter.changeCursor(null);
  }
}
