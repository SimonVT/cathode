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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.MovieSearchResult;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.BaseMoviesAdapter;
import net.simonvt.cathode.ui.adapter.MovieSearchAdapter;
import net.simonvt.cathode.ui.adapter.MovieSuggestionAdapter;
import net.simonvt.cathode.ui.adapter.SuggestionsAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.util.MovieSearchHandler;
import net.simonvt.cathode.widget.SearchView;

public class SearchMovieFragment extends ToolbarGridFragment<MovieSearchAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<Cursor>, ListDialog.Callback,
    BaseMoviesAdapter.MovieClickListener {

  private enum SortBy {
    TITLE("title", Movies.SORT_TITLE),
    RATING("rating", Movies.SORT_RATING),
    RELEVANCE("relevance", null);

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

  private static final String ARGS_QUERY = "net.simonvt.cathode.ui.SearchMovieFragment.query";

  private static final String STATE_QUERY = "net.simonvt.cathode.ui.SearchMovieFragment.query";

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.SearchMovieFragment.sortDialog";

  @Inject MovieSearchHandler searchHandler;

  @Inject Bus bus;

  private SharedPreferences settings;

  private MovieSearchAdapter movieAdapter;

  private List<Long> searchMovieIds;

  private String query;

  private MoviesNavigationListener navigationListener;

  private SortBy sortBy;

  private int columnCount;

  private Cursor cursor;

  public static Bundle getArgs(String query) {
    Bundle args = new Bundle();
    args.putString(ARGS_QUERY, query);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (MoviesNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement MoviesNavigationListener");
    }
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
    sortBy =
        SortBy.fromValue(settings.getString(Settings.SORT_SHOW_SEARCH, SortBy.RELEVANCE.getKey()));

    if (inState == null) {
      Bundle args = getArguments();
      query = args.getString(ARGS_QUERY);
      searchHandler.search(query);
    } else {
      query = inState.getString(STATE_QUERY);
      if (searchMovieIds == null && !searchHandler.isSearching()) {
        searchHandler.search(query);
      }
    }

    bus.register(this);

    columnCount = getResources().getInteger(R.integer.movieColumns);

    setTitle(query);
    updateSubtitle();
  }

  private void updateSubtitle() {
    if (searchMovieIds != null) {
      setSubtitle(getResources().getString(R.string.x_results, searchMovieIds.size()));
    } else {
      setSubtitle(null);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_QUERY, query);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public boolean displaysMenuIcon() {
    return false;
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    super.onDestroy();
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_movies_search);

    final MenuItem searchItem = toolbar.getMenu().findItem(R.id.menu_search);
    SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
    searchView.setAdapter(new MovieSuggestionAdapter(searchView.getContext()));

    searchView.setListener(new SearchView.SearchViewListener() {
      @Override public void onTextChanged(String newText) {
      }

      @Override public void onSubmit(String query) {
        navigationListener.searchMovie(query);

        MenuItemCompat.collapseActionView(searchItem);
      }

      @Override public void onSuggestionSelected(Object suggestion) {
        SuggestionsAdapter.Suggestion item = (SuggestionsAdapter.Suggestion) suggestion;
        if (item.getId() != null) {
          navigationListener.onDisplayMovie(item.getId(), item.getTitle());
        } else {
          navigationListener.searchMovie(item.getTitle());
        }

        MenuItemCompat.collapseActionView(searchItem);
      }
    });
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
        items.add(new ListDialog.Item(R.id.sort_relevance, R.string.sort_relevance));
        items.add(new ListDialog.Item(R.id.sort_rating, R.string.sort_rating));
        items.add(new ListDialog.Item(R.id.sort_title, R.string.sort_title));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return false;
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_relevance:
        sortBy = SortBy.RELEVANCE;
        settings.edit().putString(Settings.SORT_MOVIE_SEARCH, SortBy.RELEVANCE.getKey()).apply();
        if (getLoaderManager().getLoader(Loaders.LOADER_SEARCH_MOVIES) != null) {
          getLoaderManager().restartLoader(Loaders.LOADER_SEARCH_MOVIES, null, this);
        }
        break;

      case R.id.sort_rating:
        sortBy = SortBy.RATING;
        settings.edit().putString(Settings.SORT_MOVIE_SEARCH, SortBy.RATING.getKey()).apply();
        if (getLoaderManager().getLoader(Loaders.LOADER_SEARCH_MOVIES) != null) {
          getLoaderManager().restartLoader(Loaders.LOADER_SEARCH_MOVIES, null, this);
        }
        break;

      case R.id.sort_title:
        sortBy = SortBy.TITLE;
        settings.edit().putString(Settings.SORT_MOVIE_SEARCH, SortBy.TITLE.getKey()).apply();
        if (getLoaderManager().getLoader(Loaders.LOADER_SEARCH_MOVIES) != null) {
          getLoaderManager().restartLoader(Loaders.LOADER_SEARCH_MOVIES, null, this);
        }
        break;
    }
  }

  public void query(String query) {
    this.query = query;
    if (movieAdapter != null) {
      movieAdapter.changeCursor(null);
      movieAdapter = null;
      setAdapter(null);
      searchMovieIds = null;
    }
    getLoaderManager().destroyLoader(Loaders.LOADER_SEARCH_MOVIES);
    searchHandler.search(query);

    setTitle(query);
  }

  @Override public void onMovieClicked(View v, int position, long id) {
    navigationListener.onDisplayMovie(id,
        cursor.getString(cursor.getColumnIndex(MovieColumns.TITLE)));
  }

  @Subscribe public void onSearchEvent(MovieSearchResult result) {
    searchMovieIds = result.getMovieIds();
    getLoaderManager().initLoader(Loaders.LOADER_SEARCH_MOVIES, null, this);
    setEmptyText(R.string.no_results, query);
    updateSubtitle();
  }

  @Subscribe public void onSearchFailure(SearchFailureEvent event) {
    if (event.getType() == SearchFailureEvent.Type.MOVIE) {
      setCursor(null);
      setEmptyText(R.string.search_failure, query);
    }
  }

  private void setCursor(Cursor cursor) {
    this.cursor = cursor;

    if (movieAdapter == null) {
      movieAdapter = new MovieSearchAdapter(getActivity(), this, cursor);
      setAdapter(movieAdapter);
      return;
    }

    movieAdapter.changeCursor(cursor);
  }

  @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    StringBuilder where = new StringBuilder();
    where.append(MovieColumns.ID).append(" in (");
    final int showCount = searchMovieIds.size();
    String[] ids = new String[showCount];
    for (int i = 0; i < showCount; i++) {
      ids[i] = String.valueOf(searchMovieIds.get(i));

      where.append("?");
      if (i < showCount - 1) {
        where.append(",");
      }
    }
    where.append(")");

    CursorLoader loader =
        new CursorLoader(getActivity(), Movies.MOVIES, MovieSearchAdapter.PROJECTION,
            where.toString(), ids, sortBy.getSortOrder());
    loader.setUpdateThrottle(2000);
    return loader;
  }

  @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<Cursor> loader) {
  }
}
