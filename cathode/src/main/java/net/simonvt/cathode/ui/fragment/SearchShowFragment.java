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

package net.simonvt.cathode.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.search.SearchHandler;
import net.simonvt.cathode.search.ShowSearchHandler;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.ui.adapter.ShowClickListener;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.adapter.ShowSuggestionAdapter;
import net.simonvt.cathode.ui.adapter.SuggestionsAdapter;
import net.simonvt.cathode.util.Cursors;

public class SearchShowFragment extends SearchFragment {

  @Inject ShowSearchHandler searchHandler;

  private ShowsNavigationListener navigationListener;

  private int columnCount;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    columnCount = getResources().getInteger(R.integer.showsColumns);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void onSuggestionSelected(Object suggestion) {
    SuggestionsAdapter.Suggestion item = (SuggestionsAdapter.Suggestion) suggestion;
    if (item.getId() != null) {
      navigationListener.onDisplayShow(item.getId(), item.getTitle(), item.getOverview(),
          LibraryType.WATCHED);
    } else {
      query(item.getTitle());
    }
  }

  @Override public SearchFragment.SortBy getSortBy() {
    String sortBy = PreferenceManager.getDefaultSharedPreferences(getContext())
        .getString(Settings.Sort.SHOW_SEARCH, SortBy.RELEVANCE.getKey());
    return SortBy.fromValue(sortBy);
  }

  @Override public SuggestionsAdapter getSuggestionsAdapter(Context context) {
    return new ShowSuggestionAdapter(context);
  }

  @Override public SearchHandler getSearchHandler() {
    return searchHandler;
  }

  @Override public RecyclerCursorAdapter createAdapter(Cursor cursor) {
    return new ShowDescriptionAdapter(getContext(), showClickListener, cursor);
  }

  @Override public Uri getUri() {
    return Shows.SHOWS;
  }

  @Override public String[] getProjection() {
    return ShowDescriptionAdapter.PROJECTION;
  }

  @Override public String getSortString(SortBy sortBy) {
    switch (sortBy) {
      case TITLE:
        return Shows.SORT_TITLE;

      case RATING:
        return Shows.SORT_RATING;

      default:
        return null;
    }
  }

  private ShowClickListener showClickListener = new ShowClickListener() {
    @Override public void onShowClick(View view, int position, long id) {
      Cursor c = getCursorAdapter().getCursor(position);

      final String title = Cursors.getString(c, ShowColumns.TITLE);
      final String overview = Cursors.getString(c, ShowColumns.OVERVIEW);
      navigationListener.onDisplayShow(id, title, overview, LibraryType.WATCHED);
    }
  };
}
