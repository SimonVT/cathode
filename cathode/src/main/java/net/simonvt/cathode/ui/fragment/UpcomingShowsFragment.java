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
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.MutableCursor;
import net.simonvt.cathode.database.MutableCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.UpcomingAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.widget.AnimatorHelper;
import net.simonvt.cathode.widget.StaggeredGridAnimator;
import net.simonvt.cathode.widget.StaggeredGridView;

public class UpcomingShowsFragment extends StaggeredGridFragment
    implements UpcomingAdapter.OnRemoveListener, ListDialog.Callback,
    LoaderCallbacks<MutableCursor> {

  private enum SortBy {
    TITLE("title", Shows.SORT_TITLE),
    NEXT_EPISODE("nextEpisode", Shows.SORT_NEXT_EPISODE);

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

  @Inject TraktTaskQueue queue;

  private SharedPreferences settings;

  private boolean showHidden;

  private boolean isTablet;

  private SortBy sortBy;

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
    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    sortBy =
        SortBy.fromValue(settings.getString(Settings.SORT_SHOW_UPCOMING, SortBy.TITLE.getKey()));

    showHidden = settings.getBoolean(Settings.SHOW_HIDDEN, false);

    isTablet = getResources().getBoolean(R.bool.isTablet);

    setHasOptionsMenu(true);

    getLoaderManager().initLoader(BaseActivity.LOADER_SHOWS_UPCOMING, null, this);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_shows_upcoming, container, false);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_shows_upcoming);
  }

  @Override protected void onItemClick(StaggeredGridView parent, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayShow(id, c.getString(c.getColumnIndex(ShowColumns.TITLE)),
        LibraryType.WATCHED);
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
        getLoaderManager().restartLoader(BaseActivity.LOADER_SHOWS_UPCOMING, null, this);
        item.setChecked(showHidden);
        return true;

      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
        items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
        items.add(new ListDialog.Item(R.id.sort_next_episode, R.string.sort_next_episode));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

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
    Loader loader = getLoaderManager().getLoader(BaseActivity.LOADER_SHOWS_UPCOMING);
    MutableCursorLoader cursorLoader = (MutableCursorLoader) loader;
    cursorLoader.throttle(2000);
    AnimatorHelper.removeView(getGridView(), view, animatorCallback);
  }

  private AnimatorHelper.Callback animatorCallback = new AnimatorHelper.Callback() {
    @Override public void removeItem(int position) {
      int correctedPosition = ((UpcomingAdapter) getAdapter()).getCorrectedPosition(position);
      if (correctedPosition != -1) {
        MutableCursor cursor = (MutableCursor) getAdapter().getItem(position);
        cursor.remove(correctedPosition);
      }
    }

    @Override public void onAnimationEnd() {
    }
  };

  protected void setCursor(MutableCursor cursor) {
    UpcomingAdapter adapter = (UpcomingAdapter) getAdapter();
    if (adapter == null) {
      adapter = new UpcomingAdapter(getActivity(), this);
      setAdapter(adapter);
    }

    final long currentTime = System.currentTimeMillis();

    MutableCursor airedCursor = new MutableCursor(cursor.getColumnNames());
    MutableCursor unairedCursor = new MutableCursor(cursor.getColumnNames());

    final int airedIndex = cursor.getColumnIndex(EpisodeColumns.FIRST_AIRED);

    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      Object[] data = cursor.get();
      final long firstAired = cursor.getLong(airedIndex);
      if (firstAired <= currentTime) {
        airedCursor.add(data);
      } else {
        unairedCursor.add(data);
      }
    }

    StaggeredGridAnimator animator = new StaggeredGridAnimator(getGridView());
    adapter.changeCursors(airedCursor, unairedCursor);
    animator.animate();
  }

  @Override public Loader<MutableCursor> onCreateLoader(int id, Bundle args) {
    final Uri contentUri = Shows.SHOWS_UPCOMING;
    String where = null;
    if (!showHidden) {
      where = ShowColumns.HIDDEN + "=0";
    }
    MutableCursorLoader cl =
        new MutableCursorLoader(getActivity(), contentUri, UpcomingAdapter.PROJECTION, where, null,
            sortBy.getSortOrder());
    cl.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return cl;
  }

  @Override public void onLoadFinished(Loader<MutableCursor> loader, MutableCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<MutableCursor> loader) {

  }
}
