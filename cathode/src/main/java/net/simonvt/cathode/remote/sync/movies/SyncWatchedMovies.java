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
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit.Call;

public class SyncWatchedMovies extends CallJob<List<WatchedItem>> {

  @Inject transient SyncService syncService;

  public SyncWatchedMovies() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncWatchedMovies";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public Call<List<WatchedItem>> getCall() {
    return syncService.getWatchedMovies();
  }

  @Override public void handleResponse(List<WatchedItem> movies) {
    Cursor c = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.WATCHED, null, null);

    List<Long> movieIds = new ArrayList<Long>(c.getCount());

    while (c.moveToNext()) {
      movieIds.add(c.getLong(0));
    }
    c.close();

    for (WatchedItem item : movies) {
      Movie movie = item.getMovie();
      final long traktId = movie.getIds().getTrakt();
      long movieId = MovieWrapper.getMovieId(getContentResolver(), traktId);
      final long watchedAt = item.getLastWatchedAt().getTimeInMillis();

      if (movieId == -1) {
        movieId = MovieWrapper.createMovie(getContentResolver(), traktId);
        queue(new SyncMovie(traktId));
      }

      if (!movieIds.remove(movieId)) {
        MovieWrapper.setWatched(getContentResolver(), movieId, true, watchedAt);
      }
    }

    for (Long movieId : movieIds) {
      MovieWrapper.setWatched(getContentResolver(), movieId, false, 0);
    }
  }
}
