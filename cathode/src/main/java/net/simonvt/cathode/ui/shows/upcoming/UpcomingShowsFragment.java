/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.shows.upcoming;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.UpcomingTime;
import net.simonvt.cathode.settings.UpcomingTimePreference;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.HeaderSpanLookup;
import net.simonvt.cathode.ui.lists.ListDialog;
import net.simonvt.cathode.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.schematic.Cursors;

public class UpcomingShowsFragment
    extends ToolbarSwipeRefreshRecyclerFragment<RecyclerView.ViewHolder>
    implements UpcomingAdapter.OnRemoveListener, ListDialog.Callback, LoaderCallbacks<SimpleCursor>,
    UpcomingAdapter.OnItemClickListener {

  public static final String TAG = "net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment";

  private static final int LOADER_SHOWS_UPCOMING = 1;

  private enum SortBy {
    TITLE("title", Shows.SORT_TITLE),
    NEXT_EPISODE("nextEpisode", Shows.SORT_NEXT_EPISODE),
    LAST_WATCHED("lastWatched", Shows.SORT_LAST_WATCHED);

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

    private static final Map<String, SortBy> STRING_MAPPING = new HashMap<>();

    static {
      for (SortBy via : SortBy.values()) {
        STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
      }
    }

    public static SortBy fromValue(String value) {
      SortBy sortBy = STRING_MAPPING.get(value.toUpperCase(Locale.US));
      if (sortBy == null) {
        sortBy = TITLE;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.shows.upcoming.UpcomingShowsFragment.sortDialog";

  @Inject JobManager jobManager;

  @Inject UpcomingTimePreference upcomingTimePreference;

  private SharedPreferences settings;

  private SortBy sortBy;

  private ShowsNavigationListener navigationListener;

  private int columnCount;

  private UpcomingAdapter adapter;

  private boolean scrollToTop;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);
    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    sortBy =
        SortBy.fromValue(settings.getString(Settings.Sort.SHOW_UPCOMING, SortBy.TITLE.getKey()));

    getLoaderManager().initLoader(LOADER_SHOWS_UPCOMING, null, this);

    setEmptyText(R.string.empty_show_upcoming);

    columnCount = getResources().getInteger(R.integer.showsColumns);

    setTitle(R.string.title_shows_upcoming);

    upcomingTimePreference.registerListener(upcomingTimeChangeListener);
  }

  @Override public void onDestroy() {
    upcomingTimePreference.unregisterListener(upcomingTimeChangeListener);
    super.onDestroy();
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override protected GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
    return new HeaderSpanLookup(ensureAdapter(), columnCount);
  }

  private UpcomingTimePreference.UpcomingTimeChangeListener upcomingTimeChangeListener =
      new UpcomingTimePreference.UpcomingTimeChangeListener() {
        @Override public void onUpcomingTimeChanged(UpcomingTime message) {
          getLoaderManager().restartLoader(LOADER_SHOWS_UPCOMING, null, UpcomingShowsFragment.this);
        }
      };

  @Override public boolean displaysMenuIcon() {
    return amITopLevel();
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_shows_upcoming);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<>();
        items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
        items.add(new ListDialog.Item(R.id.sort_next_episode, R.string.sort_next_episode));
        items.add(new ListDialog.Item(R.id.sort_last_watched, R.string.sort_last_watched));
        ListDialog.newInstance(R.string.action_sort_by, items, UpcomingShowsFragment.this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      case R.id.menu_search:
        navigationListener.onSearchClicked();
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onShowClicked(View v, int position, long id) {
    Cursor c = ((UpcomingAdapter) getAdapter()).getCursor(position);
    final String title = Cursors.getString(c, ShowColumns.TITLE);
    final String overview = Cursors.getString(c, ShowColumns.OVERVIEW);
    navigationListener.onDisplayShow(id, title, overview, LibraryType.WATCHED);
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_title:
        if (sortBy != SortBy.TITLE) {
          sortBy = SortBy.TITLE;
          settings.edit().putString(Settings.Sort.SHOW_UPCOMING, SortBy.TITLE.getKey()).apply();
          getLoaderManager().restartLoader(LOADER_SHOWS_UPCOMING, null, this);
          scrollToTop = true;
        }
        break;

      case R.id.sort_next_episode:
        if (sortBy != SortBy.NEXT_EPISODE) {
          sortBy = SortBy.NEXT_EPISODE;
          settings.edit()
              .putString(Settings.Sort.SHOW_UPCOMING, SortBy.NEXT_EPISODE.getKey())
              .apply();
          getLoaderManager().restartLoader(LOADER_SHOWS_UPCOMING, null, this);
          scrollToTop = true;
        }
        break;

      case R.id.sort_last_watched:
        if (sortBy != SortBy.LAST_WATCHED) {
          sortBy = SortBy.LAST_WATCHED;
          settings.edit()
              .putString(Settings.Sort.SHOW_UPCOMING, SortBy.LAST_WATCHED.getKey())
              .apply();
          getLoaderManager().restartLoader(LOADER_SHOWS_UPCOMING, null, this);
          scrollToTop = true;
        }
        break;
    }
  }

  @Override public void onRemove(View view, int position, long id) {
    Loader loader = getLoaderManager().getLoader(LOADER_SHOWS_UPCOMING);
    if (loader != null) {
      SimpleCursorLoader cursorLoader = (SimpleCursorLoader) loader;
      cursorLoader.throttle(SimpleCursorLoader.DEFAULT_THROTTLE);

      List<Cursor> cursors = ((UpcomingAdapter) getAdapter()).getCursors();
      for (Cursor cursor : cursors) {
        ((SimpleCursor) cursor).remove(id);
      }

      ((UpcomingAdapter) getAdapter()).notifyChanged();
    }
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    jobManager.addJob(new SyncWatching());
    Job job = new SyncWatchedShows();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  private UpcomingAdapter ensureAdapter() {
    if (adapter == null) {
      adapter = new UpcomingAdapter(getActivity(), this, this);
      adapter.addHeader(R.string.header_aired);
      adapter.addHeader(R.string.header_upcoming);
    }

    return adapter;
  }

  protected void setCursor(SimpleCursor cursor) {
    UpcomingAdapter adapter = (UpcomingAdapter) getAdapter();
    if (adapter == null) {
      adapter = ensureAdapter();
      setAdapter(adapter);
    }

    final long currentTime = System.currentTimeMillis();

    SimpleCursor airedCursor = new SimpleCursor(cursor.getColumnNames());
    SimpleCursor unairedCursor = new SimpleCursor(cursor.getColumnNames());

    cursor.moveToPosition(-1);
    while (cursor.moveToNext()) {
      Object[] data = cursor.get();
      final long firstAired = DataHelper.getFirstAired(cursor);
      if (firstAired <= currentTime) {
        airedCursor.add(data);
      } else {
        unairedCursor.add(data);
      }
    }

    adapter.updateCursorForHeader(R.string.header_aired, airedCursor);
    adapter.updateCursorForHeader(R.string.header_upcoming, unairedCursor);

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
    return new SimpleCursorLoader(getActivity(), Shows.SHOWS_UPCOMING, UpcomingAdapter.PROJECTION,
        null, null, sortBy.getSortOrder());
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
  }
}
