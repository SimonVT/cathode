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

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.MutableCursor;
import net.simonvt.cathode.database.MutableCursorLoader;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.adapter.ShowsWithNextAdapter;
import net.simonvt.cathode.ui.adapter.UpcomingAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.widget.AnimatorHelper;

public class UpcomingShowsFragment extends ShowsFragment<MutableCursor>
    implements UpcomingAdapter.OnRemoveListener, ListDialog.Callback {

  private enum SortBy {
    TITLE("title", CathodeContract.Shows.SORT_TITLE),
    NEXT_EPISODE("nextEpisode", CathodeContract.Shows.SORT_NEXT_EPISODE);

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
      "net.simonvt.cathode.ui.fragment.UpcomingShowsFragment.sortDialog";

  private SharedPreferences settings;

  private boolean showHidden;

  private boolean isTablet;

  private MutableCursor cursor;

  private SortBy sortBy;

  @Override public void onCreate(Bundle inState) {
    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    sortBy =
        SortBy.fromValue(settings.getString(Settings.SORT_SHOW_UPCOMING, SortBy.TITLE.getKey()));
    super.onCreate(inState);
    showHidden = settings.getBoolean(Settings.SHOW_HIDDEN, false);

    isTablet = getResources().getBoolean(R.bool.isTablet);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_shows_upcoming, container, false);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_upcoming);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.fragment_shows_upcoming, menu);
    menu.findItem(R.id.menu_hidden).setChecked(showHidden);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_hidden:
        showHidden = !showHidden;
        settings.edit().putBoolean(Settings.SHOW_HIDDEN, showHidden).apply();
        getLoaderManager().restartLoader(getLoaderId(), null, this);
        item.setChecked(showHidden);
        return true;

      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
        items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
        items.add(new ListDialog.Item(R.id.sort_next_episode, R.string.sort_next_episode));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_title:
        sortBy = SortBy.TITLE;
        settings.edit().putString(Settings.SORT_SHOW_UPCOMING, SortBy.TITLE.getKey()).apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SHOWS_UPCOMING, null, this);
        break;

      case R.id.sort_next_episode:
        sortBy = SortBy.NEXT_EPISODE;
        settings.edit()
            .putString(Settings.SORT_SHOW_UPCOMING, SortBy.NEXT_EPISODE.getKey())
            .apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SHOWS_UPCOMING, null, this);
        break;
    }
  }

  @Override public void onRemove(View view, int position) {
    Loader loader = getLoaderManager().getLoader(getLoaderId());
    MutableCursorLoader cursorLoader = (MutableCursorLoader) loader;
    cursorLoader.throttle(2000);

    if (isTablet) {
      AnimatorHelper.removeView((GridView) getAdapterView(), view, animatorCallback);
    } else {
      AnimatorHelper.removeView((ListView) getAdapterView(), view, animatorCallback);
    }
  }

  private AnimatorHelper.Callback animatorCallback = new AnimatorHelper.Callback() {
    @Override public void removeItem(int position) {
      cursor.remove(position);
    }

    @Override public void onAnimationEnd() {
    }
  };

  @Override protected CursorAdapter getAdapter(Cursor cursor) {
    return new UpcomingAdapter(getActivity(), cursor, this);
  }

  @Override protected LibraryType getLibraryType() {
    return LibraryType.WATCHED;
  }

  @Override protected int getLoaderId() {
    return BaseActivity.LOADER_SHOWS_UPCOMING;
  }

  @Override protected void setCursor(Cursor cursor) {
    this.cursor = (MutableCursor) cursor;
    super.setCursor(cursor);
  }

  @Override public Loader<MutableCursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = CathodeContract.Shows.SHOWS_UPCOMING;
    String where = null;
    if (!showHidden) {
      where = CathodeContract.Shows.HIDDEN + "=0";
    }
    MutableCursorLoader cl =
        new MutableCursorLoader(getActivity(), contentUri, ShowsWithNextAdapter.PROJECTION, where,
            null, sortBy.getSortOrder());
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }

  @Override public void onLoaderReset(Loader cursorLoader) {
  }
}
