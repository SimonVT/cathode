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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.adapter.MoviesAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;

public class MovieCollectionFragment extends MoviesFragment implements ListDialog.Callback {

  private enum SortBy {
    TITLE("title", Movies.SORT_TITLE),
    COLLECTED("collected", Movies.SORT_COLLECTED);

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
        sortBy = TITLE;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.MovieCollectionFragment.sortDialog";

  private SharedPreferences settings;

  private SortBy sortBy;

  @Override public void onCreate(Bundle inState) {
    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    sortBy =
        SortBy.fromValue(settings.getString(Settings.Sort.MOVIE_COLLECTED, SortBy.TITLE.getKey()));

    super.onCreate(inState);

    setEmptyText(R.string.empty_movie_collection);
    setTitle(R.string.title_movies_collection);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    toolbar.inflateMenu(R.menu.fragment_shows_watched);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_sort) {
      ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
      items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
      items.add(new ListDialog.Item(R.id.sort_collected, R.string.sort_collected));
      ListDialog.newInstance(R.string.action_sort_by, items, this)
          .show(getFragmentManager(), DIALOG_SORT);
      return true;
    }

    return super.onMenuItemClick(item);
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_title:
        sortBy = SortBy.TITLE;
        settings.edit().putString(Settings.Sort.MOVIE_COLLECTED, SortBy.TITLE.getKey()).apply();
        getLoaderManager().restartLoader(getLoaderId(), null, this);
        break;

      case R.id.sort_collected:
        sortBy = SortBy.COLLECTED;
        settings.edit().putString(Settings.Sort.MOVIE_COLLECTED, SortBy.COLLECTED.getKey()).apply();
        getLoaderManager().restartLoader(getLoaderId(), null, this);
        break;
    }
  }

  @Override protected int getLoaderId() {
    return Loaders.MOVIES_COLLECTION;
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    SimpleCursorLoader loader =
        new SimpleCursorLoader(getActivity(), Movies.MOVIES_COLLECTED, MoviesAdapter.PROJECTION,
            null, null, sortBy.getSortOrder());
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }
}
