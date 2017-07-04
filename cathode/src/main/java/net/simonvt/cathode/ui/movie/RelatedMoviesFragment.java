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
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.cathode.ui.movies.MoviesAdapter;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.guava.Preconditions;

public class RelatedMoviesFragment
    extends ToolbarSwipeRefreshRecyclerFragment<MoviesAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<SimpleCursor>, MovieClickListener {

  private static final String TAG = "net.simonvt.cathode.ui.movie.RelatedMoviesFragment";

  private static final String ARG_MOVIE_ID =
      "net.simonvt.cathode.ui.movie.RelatedMoviesFragment.movieId";

  private static final int LOADER_MOVIES_RELATED = 1;

  @Inject JobManager jobManager;
  @Inject MovieTaskScheduler movieScheduler;

  private MoviesNavigationListener navigationListener;

  private long movieId;

  private MoviesAdapter movieAdapter;

  private SimpleCursor cursor;

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
    Injector.obtain().inject(this);

    movieId = getArguments().getLong(ARG_MOVIE_ID);

    getLoaderManager().initLoader(LOADER_MOVIES_RELATED, null, this);

    columnCount = getResources().getInteger(R.integer.movieColumns);

    setTitle(R.string.title_related);
    setEmptyText(R.string.empty_movie_related);
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

  protected void setCursor(SimpleCursor c) {
    this.cursor = c;
    if (movieAdapter == null) {
      movieAdapter = new MoviesAdapter(getActivity(), this, c, LibraryType.WATCHED);
      setAdapter(movieAdapter);
      return;
    }

    movieAdapter.changeCursor(c);
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    return new SimpleCursorLoader(getActivity(), RelatedMovies.fromMovie(movieId),
        MoviesAdapter.PROJECTION, null, null, null);
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
  }
}
