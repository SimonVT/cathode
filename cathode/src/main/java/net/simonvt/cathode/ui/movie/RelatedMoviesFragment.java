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
package net.simonvt.cathode.ui.movie;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import dagger.android.support.AndroidSupportInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.movies.BaseMoviesAdapter;
import net.simonvt.cathode.ui.movies.MoviesAdapter;

public class RelatedMoviesFragment
    extends ToolbarSwipeRefreshRecyclerFragment<MoviesAdapter.ViewHolder>
    implements BaseMoviesAdapter.Callbacks {

  private static final String TAG = "net.simonvt.cathode.ui.movie.RelatedMoviesFragment";

  private static final String ARG_MOVIE_ID =
      "net.simonvt.cathode.ui.movie.RelatedMoviesFragment.movieId";

  @Inject JobManager jobManager;
  @Inject MovieTaskScheduler movieScheduler;

  private MoviesNavigationListener navigationListener;

  private long movieId;

  private RelatedMoviesViewModel viewModel;

  private MoviesAdapter movieAdapter;

  private int columnCount;

  public static String getTag(long movieId) {
    return TAG + "/" + movieId + "/" + Ids.newId();
  }

  public static Bundle getArgs(long movieId) {
    Preconditions.checkArgument(movieId >= 0, "movieId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_MOVIE_ID, movieId);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (MoviesNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    movieId = getArguments().getLong(ARG_MOVIE_ID);

    columnCount = getResources().getInteger(R.integer.movieColumns);

    setTitle(R.string.title_related);
    setEmptyText(R.string.empty_movie_related);

    viewModel = ViewModelProviders.of(this).get(RelatedMoviesViewModel.class);
    viewModel.setMovieId(movieId);
    viewModel.getMovies().observe(this, new Observer<Cursor>() {
      @Override public void onChanged(Cursor cursor) {
        setCursor(cursor);
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
    movieScheduler.syncRelated(movieId, onDoneListener);
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

  protected void setCursor(Cursor c) {
    if (movieAdapter == null) {
      movieAdapter = new MoviesAdapter(getActivity(), this, c);
      setAdapter(movieAdapter);
      return;
    }

    movieAdapter.changeCursor(c);
  }
}
