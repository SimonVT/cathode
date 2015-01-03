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
import net.simonvt.cathode.api.entity.WatchlistItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.jobqueue.Job;

public class SyncMoviesWatchlist extends Job {

  @Inject transient SyncService syncService;

  @Override public String key() {
    return "SyncMoviesWatchlist";
  }

  @Override public int getPriority() {
    return PRIORITY_4;
  }

  @Override public void perform() {
    Cursor c = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.IN_WATCHLIST, null, null);

    List<Long> movieIds = new ArrayList<Long>();

    while (c.moveToNext()) {
      movieIds.add(c.getLong(c.getColumnIndex(MovieColumns.ID)));
    }
    c.close();

    List<WatchlistItem> watchlist = syncService.getMovieWatchlist();

    for (WatchlistItem item : watchlist) {
      final Movie movie = item.getMovie();
      final long listedAt = item.getListedAt().getTimeInMillis();
      final long traktId = movie.getIds().getTrakt();

      long movieId = MovieWrapper.getMovieId(getContentResolver(), traktId);
      if (movieId == -1L) {
        movieId = MovieWrapper.updateOrInsertMovie(getContentResolver(), movie);
        queue(new SyncMovie(traktId));
      }

      if (!movieIds.remove(movieId)) {
        MovieWrapper.setIsInWatchlist(getContentResolver(), movieId, true, listedAt);
      }
    }

    for (Long movieId : movieIds) {
      MovieWrapper.setIsInWatchlist(getContentResolver(), movieId, false);
    }
  }
}
