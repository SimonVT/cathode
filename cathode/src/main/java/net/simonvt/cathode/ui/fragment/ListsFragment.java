/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import android.text.format.DateUtils;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.ui.ListNavigationListener;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.adapter.ListsAdapter;

public class ListsFragment extends ToolbarGridFragment<ListsAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<SimpleCursor>, ListsAdapter.OnListClickListener {

  private ListNavigationListener listener;

  private ListsAdapter adapter;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    listener = (ListNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    setTitle(R.string.navigation_lists);
    setEmptyText(R.string.empty_lists);
    getLoaderManager().initLoader(Loaders.LISTS, null, this);
  }

  @Override protected int getColumnCount() {
    return 1;
  }

  @Override public void onListClicked(long listId, String listName) {
    listener.onShowList(listId, listName);
  }

  @Override public boolean displaysMenuIcon() {
    return true;
  }

  private void setCursor(Cursor cursor) {
    if (adapter == null) {
      adapter = new ListsAdapter(this, getActivity());
      setAdapter(adapter);
    }

    adapter.changeCursor(cursor);
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
    SimpleCursorLoader loader = new SimpleCursorLoader(getActivity(), ProviderSchematic.Lists.LISTS,
        ListsAdapter.PROJECTION, null, null, null);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
    setCursor(null);
  }
}
