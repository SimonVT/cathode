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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.MutableCursor;
import net.simonvt.cathode.database.MutableCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.ShowClickListener;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.adapter.ShowRecommendationsAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;

public class ShowRecommendationsFragment
    extends GridRecyclerViewFragment<ShowDescriptionAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<MutableCursor>,
    ShowRecommendationsAdapter.DismissListener, ListDialog.Callback, ShowClickListener {

  private enum SortBy {
    RELEVANCE("relevance", Shows.SORT_RECOMMENDED),
    RATING("rating", Shows.SORT_RATING);

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

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.ShowRecommendationsFragment.sortDialog";

  private ShowRecommendationsAdapter showsAdapter;

  private ShowsNavigationListener navigationListener;

  @Inject TraktTaskQueue queue;

  private boolean isTablet;

  private MutableCursor cursor;

  private SharedPreferences settings;

  private SortBy sortBy;

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

    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    sortBy = SortBy.fromValue(
        settings.getString(Settings.SORT_SHOW_RECOMMENDED, SortBy.RELEVANCE.getKey()));

    setHasOptionsMenu(true);

    getLoaderManager().initLoader(BaseActivity.LOADER_SHOWS_RECOMMENDATIONS, null, this);

    isTablet = getResources().getBoolean(R.bool.isTablet);

    columnCount = getResources().getInteger(R.integer.showsColumns);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_recommendations);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_shows, menu);
    inflater.inflate(R.menu.fragment_shows_recommended, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        queue.add(new SyncTask());
        return true;

      case R.id.menu_search:
        navigationListener.onStartShowSearch();
        return true;

      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
        items.add(new ListDialog.Item(R.id.sort_relevance, R.string.sort_relevance));
        items.add(new ListDialog.Item(R.id.sort_rating, R.string.sort_rating));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_relevance:
        sortBy = SortBy.RELEVANCE;
        settings.edit()
            .putString(Settings.SORT_SHOW_RECOMMENDED, SortBy.RELEVANCE.getKey())
            .apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SHOWS_RECOMMENDATIONS, null, this);
        break;

      case R.id.sort_rating:
        sortBy = SortBy.RATING;
        settings.edit().putString(Settings.SORT_SHOW_RECOMMENDED, SortBy.RATING.getKey()).apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SHOWS_RECOMMENDATIONS, null, this);
        break;
    }
  }

  @Override public void onShowClick(View view, int position, long id) {
    navigationListener.onDisplayShow(id, cursor.getString(cursor.getColumnIndex(ShowColumns.TITLE)),
        LibraryType.WATCHED);
  }

  @Override public void onDismissItem(final View view, final int position) {
    Loader loader = getLoaderManager().getLoader(BaseActivity.LOADER_SHOWS_RECOMMENDATIONS);
    MutableCursorLoader cursorLoader = (MutableCursorLoader) loader;
    cursorLoader.throttle(2000);

    cursor.remove(position);
    showsAdapter.notifyDataSetChanged();
  }

  private void setCursor(Cursor c) {
    cursor = (MutableCursor) c;

    if (showsAdapter == null) {
      showsAdapter = new ShowRecommendationsAdapter(getActivity(), this, cursor, this);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);
  }

  @Override public Loader<MutableCursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = Shows.SHOWS_RECOMMENDED;
    MutableCursorLoader cl =
        new MutableCursorLoader(getActivity(), contentUri, ShowDescriptionAdapter.PROJECTION,
            ShowColumns.NEEDS_SYNC, null, sortBy.getSortOrder());
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }

  @Override public void onLoadFinished(Loader<MutableCursor> loader, MutableCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<MutableCursor> loader) {
  }
}
