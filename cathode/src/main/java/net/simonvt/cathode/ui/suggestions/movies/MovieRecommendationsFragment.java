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
package net.simonvt.cathode.ui.suggestions.movies;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import dagger.android.support.AndroidSupportInjection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.ui.fragment.SwipeRefreshRecyclerFragment;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.sync.movies.SyncMovieRecommendations;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.SuggestionsTimestamps;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.lists.ListDialog;
import net.simonvt.cathode.ui.movies.BaseMoviesAdapter;
import net.simonvt.cathode.ui.movies.MoviesAdapter;

public class MovieRecommendationsFragment
    extends SwipeRefreshRecyclerFragment<MoviesAdapter.ViewHolder>
    implements BaseMoviesAdapter.Callbacks, MovieRecommendationsAdapter.DismissListener,
    ListDialog.Callback {

  enum SortBy {
    RELEVANCE("relevance", Movies.SORT_RECOMMENDED), RATING("rating", Movies.SORT_RATING);

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
      "net.simonvt.cathode.common.ui.fragment.RecommendedMoviesFragment.sortDialog";

  @Inject JobManager jobManager;

  @Inject MovieTaskScheduler movieScheduler;

  private MoviesNavigationListener navigationListener;

  private MovieRecommendationsViewModel viewModel;
  private MovieRecommendationsAdapter movieAdapter;

  private SortBy sortBy;

  private int columnCount;

  private boolean scrollToTop;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (MoviesNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    sortBy = SortBy.fromValue(Settings.get(getContext())
        .getString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RELEVANCE.getKey()));

    columnCount = getResources().getInteger(R.integer.movieColumns);

    setTitle(R.string.title_movies_recommended);
    setEmptyText(R.string.recommendations_empty);

    viewModel = ViewModelProviders.of(this).get(MovieRecommendationsViewModel.class);

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(getActivity(),
        SuggestionsTimestamps.MOVIES_RECOMMENDED)) {
      jobManager.addJob(new SyncMovieRecommendations());
      SuggestionsTimestamps.updateSuggestions(getActivity(),
          SuggestionsTimestamps.MOVIES_RECOMMENDED);
    }
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    viewModel.getRecommendations().observe(this, new Observer<List<Movie>>() {
      @Override public void onChanged(List<Movie> movies) {
        setMovies(movies);
      }
    });
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
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RELEVANCE.getKey())
              .apply();
          viewModel.setSortBy(sortBy);
          scrollToTop = true;
        }
        break;

      case R.id.sort_rating:
        if (sortBy != SortBy.RATING) {
          sortBy = SortBy.RATING;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.MOVIE_RECOMMENDED, SortBy.RATING.getKey())
              .apply();
          viewModel.setSortBy(sortBy);
          scrollToTop = true;
        }
        break;
    }
  }

  @Override public void onMovieClicked(long movieId, String title, String overview) {
    navigationListener.onDisplayMovie(movieId, title, overview);
  }

  @Override public void onCheckin(long movieId) {
    movieScheduler.checkin(movieId, null, false, false, false);
  }

  @Override public void onCancelCheckin() {
    movieScheduler.cancelCheckin();
  }

  @Override public void onWatchlistAdd(long movieId) {
    movieScheduler.setIsInWatchlist(movieId, true);
  }

  @Override public void onWatchlistRemove(long movieId) {
    movieScheduler.setIsInWatchlist(movieId, false);
  }

  @Override public void onCollectionAdd(long movieId) {
    movieScheduler.setIsInCollection(movieId, true);
  }

  @Override public void onCollectionRemove(long movieId) {
    movieScheduler.setIsInCollection(movieId, false);
  }

  @Override public void onDismissItem(final View view, Movie movie) {
    movieScheduler.dismissRecommendation(movie.getId());
    movieAdapter.removeItem(movie);
  }

  protected void setMovies(List<Movie> movies) {
    if (movieAdapter == null) {
      movieAdapter = new MovieRecommendationsAdapter(getActivity(), this, this);
      setAdapter(movieAdapter);
    }

    movieAdapter.setList(movies);

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }
}
