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
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.OnTitleChangedEvent;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.event.ShowSearchResult;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.util.ShowSearchHandler;

public class SearchShowFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String TAG = "SearchShowFragment";

  private static final String ARGS_QUERY = "net.simonvt.cathode.ui.SearchShowFragment.query";

  private static final String STATE_QUERY = "net.simonvt.cathode.ui.SearchShowFragment.query";

  @Inject ShowSearchHandler searchHandler;

  @Inject Bus bus;

  private ShowDescriptionAdapter showsAdapter;

  private List<Long> searchShowIds;

  private String query;

  private ShowsNavigationListener navigationListener;

  public static Bundle getArgs(String query) {
    Bundle args = new Bundle();
    args.putString(ARGS_QUERY, query);
    return args;
  }

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

    bus.register(this);

    if (inState == null) {
      Bundle args = getArguments();
      query = args.getString(ARGS_QUERY);
      searchHandler.search(query);
    } else {
      query = inState.getString(STATE_QUERY);
      if (searchShowIds == null && !searchHandler.isSearching()) {
        searchHandler.search(query);
      }
    }

    setHasOptionsMenu(true);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_QUERY, query);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_shows_watched, container, false);
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    super.onDestroy();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_add_show, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onStartShowSearch();
        return true;

      default:
        return false;
    }
  }

  @Override public String getTitle() {
    return query;
  }

  public void query(String query) {
    this.query = query;
    searchHandler.search(query);
    showsAdapter = null;
    setAdapter(null);
    bus.post(new OnTitleChangedEvent());
  }

  @Override protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayShow(id, c.getString(c.getColumnIndex(CathodeContract.Shows.TITLE)),
        LibraryType.WATCHED);
  }

  @Subscribe public void onSearchEvent(ShowSearchResult result) {
    searchShowIds = result.getShowIds();
    getLoaderManager().restartLoader(BaseActivity.LOADER_SEARCH_SHOWS, null, this);
    setEmptyText(R.string.no_results, query);
  }

  @Subscribe public void onSearchFailure(SearchFailureEvent event) {
    if (event.getType() == SearchFailureEvent.Type.SHOW) {
      setCursor(null);
      setEmptyText(R.string.search_failure, query);
    }
  }

  private void setCursor(Cursor cursor) {
    if (showsAdapter == null) {
      showsAdapter = new ShowDescriptionAdapter(getActivity(), cursor);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);
  }

  @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    StringBuilder where = new StringBuilder();
    where.append(CathodeContract.Shows._ID).append(" in (");
    final int showCount = searchShowIds.size();
    String[] ids = new String[showCount];
    for (int i = 0; i < showCount; i++) {
      ids[i] = String.valueOf(searchShowIds.get(i));

      where.append("?");
      if (i < showCount - 1) {
        where.append(",");
      }
    }
    where.append(")");

    CursorLoader loader = new CursorLoader(getActivity(), CathodeContract.Shows.CONTENT_URI,
        ShowDescriptionAdapter.PROJECTION, where.toString(), ids, null);

    return loader;
  }

  @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<Cursor> loader) {
  }
}
