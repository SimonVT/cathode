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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.adapter.MoviesAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;

public class TrendingMoviesFragment extends MoviesFragment implements ListDialog.Callback {

  private enum SortBy {
    VIEWERS("viewers", Movies.SORT_VIEWERS),
    RATING("rating", Movies.SORT_RATING);

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
      SortBy sortBy = STRING_MAPPING.get(value.toUpperCase());
      if (sortBy == null) {
        sortBy = VIEWERS;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.TrendingMoviesFragment.sortDialog";

  private SharedPreferences settings;

  private SortBy sortBy;

  @Override public void onCreate(Bundle inState) {
    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    sortBy =
        SortBy.fromValue(settings.getString(Settings.Sort.SHOW_TRENDING, SortBy.VIEWERS.getKey()));
    super.onCreate(inState);

    setTitle(R.string.title_movies_trending);
    setEmptyText(R.string.movies_loading_trending);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_movies_trending);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
        items.add(new ListDialog.Item(R.id.sort_viewers, R.string.sort_viewers));
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
      case R.id.sort_viewers:
        sortBy = SortBy.VIEWERS;
        settings.edit().putString(Settings.Sort.MOVIE_TRENDING, SortBy.VIEWERS.getKey()).apply();
        getLoaderManager().restartLoader(Loaders.LOADER_MOVIES_TRENDING, null, this);
        break;

      case R.id.sort_rating:
        sortBy = SortBy.RATING;
        settings.edit().putString(Settings.Sort.MOVIE_TRENDING, SortBy.RATING.getKey()).apply();
        getLoaderManager().restartLoader(Loaders.LOADER_MOVIES_TRENDING, null, this);
        break;
    }
  }

  protected RecyclerView.Adapter<MoviesAdapter.ViewHolder> getAdapter(Cursor cursor) {
    return new MoviesAdapter(getActivity(), this, cursor, R.layout.list_row_movie_rating);
  }

  @Override protected int getLoaderId() {
    return Loaders.LOADER_MOVIES_TRENDING;
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    SimpleCursorLoader loader =
        new SimpleCursorLoader(getActivity(), Movies.TRENDING, null, MovieColumns.NEEDS_SYNC + "=0",
            null, sortBy.getSortOrder());
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }
}
