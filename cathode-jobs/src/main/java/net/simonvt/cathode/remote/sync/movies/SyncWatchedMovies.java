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
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.WatchedItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class SyncWatchedMovies extends CallJob<List<WatchedItem>> {

  @Inject transient SyncService syncService;
  @Inject transient MovieDatabaseHelper movieHelper;

  public SyncWatchedMovies() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncWatchedMovies";
  }

  @Override public int getPriority() {
    return JobPriority.USER_DATA;
  }

  @Override public Call<List<WatchedItem>> getCall() {
    return syncService.getWatchedMovies();
  }

  @Override public boolean handleResponse(List<WatchedItem> movies) {
    Cursor c = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.WATCHED, null, null);

    List<Long> movieIds = new ArrayList<>(c.getCount());

    while (c.moveToNext()) {
      movieIds.add(c.getLong(0));
    }
    c.close();

    for (WatchedItem item : movies) {
      Movie movie = item.getMovie();
      final long traktId = movie.getIds().getTrakt();
      MovieDatabaseHelper.IdResult result = movieHelper.getIdOrCreate(traktId);
      final long movieId = result.movieId;
      final long watchedAt = item.getLastWatchedAt().getTimeInMillis();

      if (!movieIds.remove(movieId)) {
        movieHelper.addToHistory(movieId, watchedAt);

        if (movieHelper.needsSync(movieId)) {
          queue(new SyncMovie(traktId));
        }
      }
    }

    for (Long movieId : movieIds) {
      movieHelper.removeFromHistory(movieId);
    }

    return true;
  }
}
