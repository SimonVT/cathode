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
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.MutableCursor;
import net.simonvt.cathode.database.MutableCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.MovieRecommendationsAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.widget.AdapterViewAnimator;
import net.simonvt.cathode.widget.AnimatorHelper;
import net.simonvt.cathode.widget.DefaultAdapterAnimator;

public class MovieRecommendationsFragment extends AbsAdapterFragment
    implements LoaderManager.LoaderCallbacks<MutableCursor>,
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
      return STRING_MAPPING.get(value.toUpperCase());
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.fragment.RecommendedMoviesFragment.sortDialog";

  @Inject TraktTaskQueue queue;

  private MoviesNavigationListener navigationListener;

  private boolean isTablet;

  private MovieRecommendationsAdapter movieAdapter;

  private MutableCursor cursor;

  private SharedPreferences settings;

  private SortBy sortBy;

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
        settings.getString(Settings.SORT_SHOW_RECOMMENDED, SortBy.RELEVANCE.getKey()));

    setHasOptionsMenu(true);

    getLoaderManager().initLoader(BaseActivity.LOADER_MOVIES_RECOMMENDATIONS, null, this);

    isTablet = getResources().getBoolean(R.bool.isTablet);
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.title_movies_recommendations);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_list_cards, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    setEmptyText(R.string.movies_loading_trending);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.fragment_movies, menu);
    inflater.inflate(R.menu.fragment_movies_recommended, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_refresh:
        queue.add(new SyncTask());
        return true;

      case R.id.menu_search:
        navigationListener.onStartMovieSearch();
        return true;

      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<ListDialog.Item>();
        items.add(new ListDialog.Item(R.id.sort_relevance, R.string.sort_relevance));
        items.add(new ListDialog.Item(R.id.sort_rating, R.string.sort_rating));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_relevance:
        sortBy = SortBy.RELEVANCE;
        settings.edit()
            .putString(Settings.SORT_MOVIE_RECOMMENDED, SortBy.RELEVANCE.getKey())
            .apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_MOVIES_RECOMMENDATIONS, null, this);
        break;

      case R.id.sort_rating:
        sortBy = SortBy.RATING;
        settings.edit().putString(Settings.SORT_MOVIE_RECOMMENDED, SortBy.RATING.getKey()).apply();
        getLoaderManager().restartLoader(BaseActivity.LOADER_MOVIES_RECOMMENDATIONS, null, this);
        break;
    }
  }

  @Override protected void onItemClick(AdapterView l, View v, int position, long id) {
    Cursor c = (Cursor) getAdapter().getItem(position);
    navigationListener.onDisplayMovie(id, c.getString(c.getColumnIndex(MovieColumns.TITLE)));
  }

  @Override public void onDismissItem(final View view, final int position) {
    Loader loader = getLoaderManager().getLoader(BaseActivity.LOADER_MOVIES_RECOMMENDATIONS);
    MutableCursorLoader cursorLoader = (MutableCursorLoader) loader;
    cursorLoader.throttle(2000);

    if (isTablet) {
      AnimatorHelper.removeView((GridView) getAdapterView(), view, animatorCallback);
    } else {
      AnimatorHelper.removeView((ListView) getAdapterView(), view, animatorCallback);
    }
  }

  private AnimatorHelper.Callback animatorCallback = new AnimatorHelper.Callback() {
    @Override public void removeItem(int position) {
      cursor.remove(position);
    }

    @Override public void onAnimationEnd() {
    }
  };

  protected void setCursor(MutableCursor c) {
    this.cursor = c;
    if (movieAdapter == null) {
      movieAdapter = new MovieRecommendationsAdapter(getActivity(), c, this);
      setAdapter(movieAdapter);
      return;
    }

    AdapterViewAnimator animator =
        new AdapterViewAnimator(adapterView, new DefaultAdapterAnimator());
    movieAdapter.changeCursor(c);
    animator.animate();
  }

  @Override public Loader<MutableCursor> onCreateLoader(int i, Bundle bundle) {
    MutableCursorLoader loader =
        new MutableCursorLoader(getActivity(), Movies.RECOMMENDED, null, null, null,
            sortBy.getSortOrder());
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }

  @Override public void onLoadFinished(Loader<MutableCursor> loader, MutableCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<MutableCursor> loader) {
    setAdapter(null);
  }
}
