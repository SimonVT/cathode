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
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.simonvt.cathode.remote.sync.shows.SyncShowRecommendations;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.ShowClickListener;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.adapter.ShowRecommendationsAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.schematic.Cursors;

public class ShowRecommendationsFragment
    extends SwipeRefreshRecyclerFragment<ShowDescriptionAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<SimpleCursor>,
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

    private static final Map<String, SortBy> STRING_MAPPING = new HashMap<>();

    static {
      for (SortBy via : SortBy.values()) {
        STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
      }
    }

    public static SortBy fromValue(String value) {
      SortBy sortBy = STRING_MAPPING.get(value.toUpperCase(Locale.US));
      if (sortBy == null) {
        sortBy = RELEVANCE;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.ShowRecommendationsFragment.sortDialog";

  private ShowRecommendationsAdapter showsAdapter;

  private ShowsNavigationListener navigationListener;

  @Inject JobManager jobManager;

  private boolean isTablet;

  private SimpleCursor cursor;

  private SharedPreferences settings;

  private SortBy sortBy;

  private int columnCount;

  private boolean scrollToTop;

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
        settings.getString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RELEVANCE.getKey()));

    getLoaderManager().initLoader(Loaders.SHOWS_RECOMMENDATIONS, null, this);

    isTablet = getResources().getBoolean(R.bool.isTablet);

    columnCount = getResources().getInteger(R.integer.showsColumns);

    setTitle(R.string.title_shows_recommendations);
    setEmptyText(R.string.recommendations_empty);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    Job job = new SyncShowRecommendations();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<>();
        items.add(new ListDialog.Item(R.id.sort_relevance, R.string.sort_relevance));
        items.add(new ListDialog.Item(R.id.sort_rating, R.string.sort_rating));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_relevance:
        if (sortBy != SortBy.RELEVANCE) {
          sortBy = SortBy.RELEVANCE;
          settings.edit()
              .putString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RELEVANCE.getKey())
              .apply();
          getLoaderManager().restartLoader(Loaders.SHOWS_RECOMMENDATIONS, null, this);
          scrollToTop = true;
        }
        break;

      case R.id.sort_rating:
        if (sortBy != SortBy.RATING) {
          sortBy = SortBy.RATING;
          settings.edit().putString(Settings.Sort.SHOW_RECOMMENDED, SortBy.RATING.getKey()).apply();
          getLoaderManager().restartLoader(Loaders.SHOWS_RECOMMENDATIONS, null, this);
          scrollToTop = true;
        }
        break;
    }
  }

  @Override public void onShowClick(View view, int position, long id) {
    cursor.moveToPosition(position);
    final String title = Cursors.getString(cursor, ShowColumns.TITLE);
    final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
    navigationListener.onDisplayShow(id, title, overview, LibraryType.WATCHED);
  }

  @Override public void onDismissItem(final View view, final long id) {
    Loader loader = getLoaderManager().getLoader(Loaders.SHOWS_RECOMMENDATIONS);
    SimpleCursorLoader cursorLoader = (SimpleCursorLoader) loader;
    cursorLoader.throttle(SimpleCursorLoader.DEFAULT_THROTTLE);

    cursor.remove(id);
    showsAdapter.notifyDataSetChanged();
  }

  private void setCursor(Cursor c) {
    cursor = (SimpleCursor) c;

    if (showsAdapter == null) {
      showsAdapter = new ShowRecommendationsAdapter(getActivity(), this, cursor, this);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    final Uri contentUri = Shows.SHOWS_RECOMMENDED;
    return new SimpleCursorLoader(getActivity(), contentUri, ShowDescriptionAdapter.PROJECTION, null,
        null, sortBy.getSortOrder());
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
  }
}
