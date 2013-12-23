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
package net.simonvt.cathode.remote.sync;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncMoviesWatchlistTask extends TraktTask {

  private static final String TAG = "SyncMoviesWatchlistTask";

  @Inject transient UserService userService;

  @Override protected void doTask() {
    try {
      Cursor c =
          service.getContentResolver().query(CathodeContract.Movies.CONTENT_URI, new String[] {
              CathodeContract.Movies._ID,
          }, CathodeContract.Movies.IN_WATCHLIST, null, null);

      List<Long> movieIds = new ArrayList<Long>();

      while (c.moveToNext()) {
        movieIds.add(c.getLong(c.getColumnIndex(CathodeContract.Movies._ID)));
      }
      c.close();

      List<Movie> movies = userService.watchlistMovies();

      for (Movie movie : movies) {
        if (movie.getTmdbId() == null) {
          continue;
        }
        final long tmdbId = movie.getTmdbId();
        final long movieId = MovieWrapper.getMovieId(service.getContentResolver(), tmdbId);

        if (movieId == -1) {
          queueTask(new SyncMovieTask(tmdbId));
        } else {
          if (!movieIds.remove(movieId)) {
            MovieWrapper.setIsInWatchlist(service.getContentResolver(), movieId, true);
          }
        }
      }

      for (Long movieId : movieIds) {
        MovieWrapper.setIsInWatchlist(service.getContentResolver(), movieId, false);
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
