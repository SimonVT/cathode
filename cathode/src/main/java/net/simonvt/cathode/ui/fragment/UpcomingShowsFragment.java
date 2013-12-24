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
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.adapter.ShowsWithNextAdapter;
import net.simonvt.cathode.ui.adapter.UpcomingAdapter;

public class UpcomingShowsFragment extends ShowsFragment<Cursor> {

  private static final String DIALOG_HIDDEN =
      "net.simonvt.cathode.ui.fragment.UpcomingShowsFragment.hiddenShowsDialog";

  private SharedPreferences settings;

  private boolean showHidden;

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    showHidden = settings.getBoolean(Settings.SHOW_HIDDEN, false);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_shows_upcoming, container, false);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_upcoming);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    menu.add(0, R.id.menu_hidden, getResources().getInteger(R.integer.order_menu_hidden),
        R.string.action_show_hidden)
        .setCheckable(true)
        .setChecked(showHidden)
        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_hidden:
        showHidden = !showHidden;
        settings.edit().putBoolean(Settings.SHOW_HIDDEN, showHidden).apply();
        getLoaderManager().restartLoader(getLoaderId(), null, this);
        item.setChecked(showHidden);
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override protected CursorAdapter getAdapter(Cursor cursor) {
    return new UpcomingAdapter(getActivity(), cursor);
  }

  @Override protected LibraryType getLibraryType() {
    return LibraryType.WATCHED;
  }

  @Override protected int getLoaderId() {
    return BaseActivity.LOADER_SHOWS_UPCOMING;
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = CathodeContract.Shows.SHOWS_UPCOMING;
    String where = null;
    if (!showHidden) {
      where = CathodeContract.Shows.HIDDEN + "=0";
    }
    CursorLoader cl =
        new CursorLoader(getActivity(), contentUri, ShowsWithNextAdapter.PROJECTION, where, null,
            CathodeContract.Shows.DEFAULT_SORT);
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }

  @Override public void onLoaderReset(Loader cursorLoader) {
  }
}
