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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import net.simonvt.cathode.event.OnTitleChangedEvent;
import net.simonvt.cathode.event.SearchFailureEvent;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.MovieSearchAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.util.MovieSearchHandler;
import net.simonvt.cathode.widget.AdapterViewAnimator;
import net.simonvt.cathode.widget.DefaultAdapterAnimator;

public class SearchMovieFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<Cursor>, ListDialog.Callback {

  private enum SortBy {
    TITLE("title", CathodeContract.Movies.SORT_TITLE),
    RATING("rating", CathodeContract.Movies.SORT_RATING),
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
    setHasOptionsMenu(true);
  }

  @Override public String getTitle() {
    return query;
  }

  @Override public String getSubtitle() {
    return searchMovieIds != null ? getResources().getString(R.string.x_results,
        searchMovieIds.size()) : null;
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_QUERY, query);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_movies_watched, container, false);
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    super.onDestroy();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_movies_search, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onStartMovieSearch();
        return true;

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
        getLoaderManager().restartLoader(BaseActivity.LOADER_SEARCH_MOVIES, null, this);
        break;

      case R.id.sort_rating:
        sortBy = SortBy.RATING;
        settings.edit().putString(Settings.SORT_MOVIE_SEARCH, SortBy.RATING.getKey()).apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SEARCH_MOVIES, null, this);
        break;

      case R.id.sort_title:
        sortBy = SortBy.TITLE;
        settings.edit().putString(Settings.SORT_MOVIE_SEARCH, SortBy.TITLE.getKey()).apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_SEARCH_MOVIES, null, this);
        break;
    }
  }

  public void query(String query) {
    this.query = query;
    searchHandler.search(query);
    movieAdapter = null;
    setAdapter(null);
    bus.post(new OnTitleChangedEvent());
  }

  @Override protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayMovie(id,
        c.getString(c.getColumnIndex(CathodeContract.Movies.TITLE)));
  }

  @Subscribe public void onSearchEvent(MovieSearchResult result) {
    searchMovieIds = result.getMovieIds();
    getLoaderManager().restartLoader(BaseActivity.LOADER_SEARCH_MOVIES, null, this);
    setEmptyText(R.string.no_results, query);
    bus.post(new OnTitleChangedEvent());
  }

  @Subscribe public void onSearchFailure(SearchFailureEvent event) {
    if (event.getType() == SearchFailureEvent.Type.MOVIE) {
      setCursor(null);
      setEmptyText(R.string.search_failure, query);
      bus.post(new OnTitleChangedEvent());
    }
  }

  private void setCursor(Cursor cursor) {
    if (movieAdapter == null) {
      movieAdapter = new MovieSearchAdapter(getActivity(), cursor);
      setAdapter(movieAdapter);
      return;
    }

    AdapterViewAnimator animator =
        new AdapterViewAnimator(adapterView, new DefaultAdapterAnimator());
    movieAdapter.changeCursor(cursor);
    animator.animate();
  }

  @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    StringBuilder where = new StringBuilder();
    where.append(CathodeContract.Movies._ID).append(" in (");
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

    CursorLoader loader = new CursorLoader(getActivity(), CathodeContract.Movies.CONTENT_URI,
        MovieSearchAdapter.PROJECTION, where.toString(), ids, sortBy.getSortOrder());
    loader.setUpdateThrottle(2000);
    return loader;
  }

  @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<Cursor> loader) {
  }
}
