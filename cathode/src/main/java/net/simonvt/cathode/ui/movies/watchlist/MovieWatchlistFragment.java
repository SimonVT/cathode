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
package net.simonvt.cathode.ui.movies.watchlist;

import android.os.Bundle;
import androidx.loader.content.Loader;
import net.simonvt.cathode.R;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.database.SimpleCursor;
import net.simonvt.cathode.provider.database.SimpleCursorLoader;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesWatchlist;
import net.simonvt.cathode.ui.movies.MoviesFragment;

public class MovieWatchlistFragment extends MoviesFragment {

  public static final String TAG = "net.simonvt.cathode.ui.movies.watchlist.MovieWatchlistFragment";

  private static final int LOADER_MOVIES_WATCHLIST = 1;

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    setEmptyText(R.string.empty_movie_watchlist);
    setTitle(R.string.title_movies_watchlist);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    Job job = new SyncMoviesWatchlist();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  @Override protected int getLoaderId() {
    return LOADER_MOVIES_WATCHLIST;
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    return new SimpleCursorLoader(getActivity(), Movies.MOVIES_WATCHLIST, null, null, null,
        Movies.DEFAULT_SORT);
  }
}
