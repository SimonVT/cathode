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
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.sync.movies.SyncMovieRecommendations;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.adapter.MovieRecommendationsAdapter;
import net.simonvt.cathode.ui.adapter.MoviesAdapter;
import net.simonvt.cathode.ui.dialog.ListDialog;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.schematic.Cursors;

public class MovieRecommendationsFragment
    extends SwipeRefreshRecyclerFragment<MoviesAdapter.ViewHolder>
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
      "net.simonvt.cathode.ui.fragment.RecommendedMoviesFragment.sortDialog";

  @Inject JobManager jobManager;

  private MoviesNavigationListener navigationListener;

  private MovieRecommendationsAdapter movieAdapter;

  private SimpleCursor cursor;

  private SharedPreferences settings;

  private SortBy sortBy;

  private int columnCount;

  private boolean scrollToTop;

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

    getLoaderManager().initLoader(Loaders.MOVIES_RECOMMENDATIONS, null, this);

    columnCount = getResources().getInteger(R.integer.movieColumns);

    setTitle(R.string.title_movies_recommended);
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
    Job job = new SyncMovieRecommendations();
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
              .putString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RELEVANCE.getKey())
              .apply();
          getLoaderManager().restartLoader(Loaders.MOVIES_RECOMMENDATIONS, null, this);
          scrollToTop = true;
        }
        break;

      case R.id.sort_rating:
        if (sortBy != SortBy.RATING) {
          sortBy = SortBy.RATING;
          settings.edit()
              .putString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RATING.getKey())
              .apply();
          getLoaderManager().restartLoader(Loaders.MOVIES_RECOMMENDATIONS, null, this);
          scrollToTop = true;
        }
        break;
    }
  }

  @Override public void onMovieClicked(View view, int position, long id) {
    cursor.moveToPosition(position);
    final String title = Cursors.getString(cursor, MovieColumns.TITLE);
    final String overview = Cursors.getString(cursor, MovieColumns.OVERVIEW);
    navigationListener.onDisplayMovie(id, title, overview);
  }

  @Override public void onDismissItem(final View view, final long id) {
    Loader loader = getLoaderManager().getLoader(Loaders.MOVIES_RECOMMENDATIONS);
    if (loader != null) {
      SimpleCursorLoader cursorLoader = (SimpleCursorLoader) loader;
      cursorLoader.throttle(SimpleCursorLoader.DEFAULT_THROTTLE);

      cursor.remove(id);
      movieAdapter.notifyDataSetChanged();
    }
  }

  protected void setCursor(SimpleCursor c) {
    this.cursor = c;
    if (movieAdapter == null) {
      movieAdapter = new MovieRecommendationsAdapter(getActivity(), this, c, this);
      setAdapter(movieAdapter);
      return;
    }

    movieAdapter.changeCursor(c);

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    return new SimpleCursorLoader(getActivity(), Movies.RECOMMENDED, null,
        MovieColumns.NEEDS_SYNC + "=0", null, sortBy.getSortOrder());
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
    setAdapter(null);
  }
}
