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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.OnTitleChangedEvent;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.event.ShowSearchResult;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.ShowClickListener;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.util.ShowSearchHandler;

public class SearchShowFragment extends GridRecyclerViewFragment<ShowDescriptionAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<Cursor>, ListDialog.Callback, ShowClickListener {

  private enum SortBy {
    TITLE("title", Shows.SORT_TITLE),
    RATING("rating", Shows.SORT_RATING),
    RELEVANCE("relevance", null);

    private String key;

    private String sortOrder;

    SortBy(String key, String sortOrder) {
      this.key = key;
      this.sortOrder = sortOrder;
    }

    public String getKey() {
      return key;
    }

    public String getSortOrder() {
      return sortOrder;
    }

    @Override public String toString() {
      return key;
    }

    private static final Map<String, SortBy> STRING_MAPPING = new HashMap<String, SortBy>();

    static {
      for (SortBy via : SortBy.values()) {
        STRING_MAPPING.put(via.toString().toUpperCase(), via);
      }
    }

    public static SortBy fromValue(String value) {
      return STRING_MAPPING.get(value.toUpperCase());
    }
  }

  private static final String ARGS_QUERY = "net.simonvt.cathode.ui.SearchShowFragment.query";

  private static final String STATE_QUERY = "net.simonvt.cathode.ui.SearchShowFragment.query";

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.UpcomingShowsFragment.sortDialog";

  @Inject ShowSearchHandler searchHandler;

  @Inject Bus bus;

  private SharedPreferences settings;

  private ShowDescriptionAdapter showsAdapter;

  private List<Long> searchShowIds;

  private String query;

  private ShowsNavigationListener navigationListener;

  private SortBy sortBy;

  private int columnCount;

  private Cursor cursor;

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

    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    sortBy =
        SortBy.fromValue(settings.getString(Settings.SORT_SHOW_SEARCH, SortBy.RELEVANCE.getKey()));

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

    bus.register(this);

    setHasOptionsMenu(true);

    columnCount = getResources().getInteger(R.integer.showsColumns);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_QUERY, query);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    super.onDestroy();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_show_search, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onStartShowSearch();
        return true;

      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
        items.add(new ListDialog.Item(R.id.sort_relevance, R.string.sort_relevance));
        items.add(new ListDialog.Item(R.id.sort_rating, R.string.sort_rating));
        items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return false;
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_relevance:
        sortBy = SortBy.RELEVANCE;
        settings.edit().putString(Settings.SORT_SHOW_SEARCH, SortBy.RELEVANCE.getKey()).apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SEARCH_SHOWS, null, this);
        break;

      case R.id.sort_rating:
        sortBy = SortBy.RATING;
        settings.edit().putString(Settings.SORT_SHOW_SEARCH, SortBy.RATING.getKey()).apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SEARCH_SHOWS, null, this);
        break;

      case R.id.sort_title:
        sortBy = SortBy.TITLE;
        settings.edit().putString(Settings.SORT_SHOW_SEARCH, SortBy.TITLE.getKey()).apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SEARCH_SHOWS, null, this);
        break;
    }
  }

  @Override public String getTitle() {
    return query;
  }

  @Override public String getSubtitle() {
    return searchShowIds != null ? getResources().getString(R.string.x_results,
        searchShowIds.size()) : null;
  }

  public void query(String query) {
    this.query = query;
    if (showsAdapter != null) {
      showsAdapter.changeCursor(null);
      showsAdapter = null;
      setAdapter(null);
      searchShowIds = null;
    }
    getLoaderManager().destroyLoader(BaseActivity.LOADER_SEARCH_SHOWS);
    searchHandler.search(query);
    bus.post(new OnTitleChangedEvent());
  }

  @Override public void onShowClick(View view, int position, long id) {
    navigationListener.onDisplayShow(id, cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE)),
        LibraryType.WATCHED);
  }

  @Subscribe public void onSearchEvent(ShowSearchResult result) {
    searchShowIds = result.getShowIds();
    getLoaderManager().initLoader(BaseActivity.LOADER_SEARCH_SHOWS, null, this);
    setEmptyText(R.string.no_results, query);
    bus.post(new OnTitleChangedEvent());
  }

  @Subscribe public void onSearchFailure(SearchFailureEvent event) {
    if (event.getType() == SearchFailureEvent.Type.SHOW) {
      setCursor(null);
      setEmptyText(R.string.search_failure, query);
      bus.post(new OnTitleChangedEvent());
    }
  }

  private void setCursor(Cursor cursor) {
    this.cursor = cursor;
    if (showsAdapter == null) {
      showsAdapter = new ShowDescriptionAdapter(getActivity(), this, cursor);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);
  }

  @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    StringBuilder where = new StringBuilder();
    where.append(ShowColumns.ID).append(" in (");
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

    CursorLoader loader =
        new CursorLoader(getActivity(), Shows.SHOWS, ShowDescriptionAdapter.PROJECTION,
            where.toString(), ids, sortBy.getSortOrder());
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }

  @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<Cursor> loader) {
  }
}
