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
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.search.MovieSearchHandler;
import net.simonvt.cathode.search.SearchHandler;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.MovieSearchAdapter;
import net.simonvt.cathode.ui.adapter.MovieSuggestionAdapter;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.ui.adapter.SuggestionsAdapter;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.schematic.Cursors;

public class SearchMovieFragment extends SearchFragment {

  @Inject MovieSearchHandler searchHandler;

  private MoviesNavigationListener navigationListener;

  private int columnCount;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (MoviesNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    searchHandler.addListener(this);

    columnCount = getResources().getInteger(R.integer.movieColumns);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void onSuggestionSelected(Object suggestion) {
    SuggestionsAdapter.Suggestion item = (SuggestionsAdapter.Suggestion) suggestion;
    if (item.getId() != null) {
      navigationListener.onDisplayMovie(item.getId(), item.getTitle(), item.getOverview());
    } else {
      query(item.getTitle());
    }
  }

  @Override public SearchFragment.SortBy getSortBy() {
    String sortBy = PreferenceManager.getDefaultSharedPreferences(getContext())
        .getString(Settings.Sort.MOVIE_SEARCH, SortBy.RELEVANCE.getKey());
    return SortBy.fromValue(sortBy);
  }

  @Override public SuggestionsAdapter getSuggestionsAdapter(Context context) {
    return new MovieSuggestionAdapter(context);
  }

  @Override public SearchHandler getSearchHandler() {
    return searchHandler;
  }

  @Override public RecyclerCursorAdapter createAdapter(Cursor cursor) {
    return new MovieSearchAdapter(getActivity(), movieClickListener, cursor);
  }

  private MovieClickListener movieClickListener = new MovieClickListener() {
    @Override public void onMovieClicked(View v, int position, long id) {
      Cursor c = getCursorAdapter().getCursor(position);
      final String title = Cursors.getString(c, MovieColumns.TITLE);
      final String overview = Cursors.getString(c, MovieColumns.OVERVIEW);
      navigationListener.onDisplayMovie(id, title, overview);
    }
  };

  @Override public Uri getUri() {
    return Movies.MOVIES;
  }

  @Override public String[] getProjection() {
    return MovieSearchAdapter.PROJECTION;
  }

  @Override public String getSortString(SortBy sortBy) {
    switch (sortBy) {
      case TITLE:
        return Movies.SORT_TITLE;

      case RATING:
        return Movies.SORT_RATING;

      default:
        return null;
    }
  }
}
