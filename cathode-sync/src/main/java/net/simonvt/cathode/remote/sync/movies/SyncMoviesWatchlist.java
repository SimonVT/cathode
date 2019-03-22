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
package net.simonvt.cathode.remote.sync.movies;

import android.database.Cursor;
import androidx.work.WorkManager;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.WatchlistItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.work.WorkManagerUtils;
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker;
import retrofit2.Call;

public class SyncMoviesWatchlist extends CallJob<List<WatchlistItem>> {

  @Inject transient WorkManager workManager;

  @Inject transient SyncService syncService;
  @Inject transient MovieDatabaseHelper movieHelper;

  public SyncMoviesWatchlist() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncMoviesWatchlist";
  }

  @Override public int getPriority() {
    return JobPriority.USER_DATA;
  }

  @Override public Call<List<WatchlistItem>> getCall() {
    return syncService.getMovieWatchlist();
  }

  @Override public boolean handleResponse(List<WatchlistItem> watchlist) {
    Cursor c = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.IN_WATCHLIST, null, null);

    List<Long> movieIds = new ArrayList<>();

    while (c.moveToNext()) {
      movieIds.add(Cursors.getLong(c, MovieColumns.ID));
    }
    c.close();

    for (WatchlistItem item : watchlist) {
      final Movie movie = item.getMovie();
      final long listedAt = item.getListedAt().getTimeInMillis();
      final long traktId = movie.getIds().getTrakt();

      MovieDatabaseHelper.IdResult result = movieHelper.getIdOrCreate(traktId);
      final long movieId = result.movieId;

      if (!movieIds.remove(movieId)) {
        movieHelper.setIsInWatchlist(movieId, true, listedAt);
      }
    }

    for (Long movieId : movieIds) {
      movieHelper.setIsInWatchlist(movieId, false);
    }

    WorkManagerUtils.enqueueUniqueNow(workManager, SyncPendingMoviesWorker.TAG, SyncPendingMoviesWorker.class);

    return true;
  }
}
