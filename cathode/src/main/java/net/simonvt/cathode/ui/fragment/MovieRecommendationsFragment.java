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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.MovieRecommendationsAdapter;
import net.simonvt.cathode.ui.adapter.MovieSuggestionAdapter;
import net.simonvt.cathode.ui.adapter.MoviesAdapter;
import net.simonvt.cathode.ui.adapter.SuggestionsAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.cathode.widget.SearchView;

public class MovieRecommendationsFragment extends ToolbarGridFragment<MoviesAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<SimpleCursor>, MovieClickListener,
    MovieRecommendationsAdapter.DismissListener, ListDialog.Callback {

  private enum SortBy {
    RELEVANCE("relevance", Movies.SORT_RECOMMENDED),
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
        sortBy = RELEVANCE;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.RecommendedMoviesFragment.sortDialog";

  @Inject JobManager jobManager;

  private MoviesNavigationListener navigationListener;

  private MovieRecommendationsAdapter movieAdapter;

  private SimpleCursor cursor;

  private SharedPreferences settings;

  private SortBy sortBy;

  private int columnCount;

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
    sortBy = SortBy.fromValue(
        settings.getString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RELEVANCE.getKey()));

    getLoaderManager().initLoader(Loaders.LOADER_MOVIES_RECOMMENDATIONS, null, this);

    columnCount = getResources().getInteger(R.integer.movieColumns);

    setTitle(R.string.title_movies_recommendations);
    setEmptyText(R.string.recommendations_empty);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public boolean displaysMenuIcon() {
    return true;
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_movies);
    toolbar.inflateMenu(R.menu.fragment_movies_recommended);

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
      case R.id.menu_refresh:
        jobManager.addJob(new SyncJob());
        return true;

      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
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
        sortBy = SortBy.RELEVANCE;
        settings.edit()
            .putString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RELEVANCE.getKey())
            .apply();
        getLoaderManager().restartLoader(Loaders.LOADER_MOVIES_RECOMMENDATIONS, null, this);
        break;

      case R.id.sort_rating:
        sortBy = SortBy.RATING;
        settings.edit().putString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RATING.getKey()).apply();
        getLoaderManager().restartLoader(Loaders.LOADER_MOVIES_RECOMMENDATIONS, null, this);
        break;
    }
  }

  @Override public void onMovieClicked(View view, int position, long movieId) {
    cursor.moveToPosition(position);
    navigationListener.onDisplayMovie(movieId,
        cursor.getString(cursor.getColumnIndex(MovieColumns.TITLE)));
  }

  @Override public void onDismissItem(final View view, final long id) {
    Loader loader = getLoaderManager().getLoader(Loaders.LOADER_MOVIES_RECOMMENDATIONS);
    SimpleCursorLoader cursorLoader = (SimpleCursorLoader) loader;
    cursorLoader.throttle(2000);

    cursor.remove(id);
    movieAdapter.notifyDataSetChanged();
  }

  protected void setCursor(SimpleCursor c) {
    this.cursor = c;
    if (movieAdapter == null) {
      movieAdapter = new MovieRecommendationsAdapter(getActivity(), this, c, this);
      setAdapter(movieAdapter);
      return;
    }

    movieAdapter.changeCursor(c);
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    SimpleCursorLoader loader = new SimpleCursorLoader(getActivity(), Movies.RECOMMENDED, null,
        MovieColumns.NEEDS_SYNC + "=0", null, sortBy.getSortOrder());
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
    setAdapter(null);
  }
}
