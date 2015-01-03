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
package net.simonvt.cathode.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.action.CancelCheckin;
import net.simonvt.cathode.remote.action.movies.CheckInMovie;
import net.simonvt.cathode.remote.action.movies.CollectMovie;
import net.simonvt.cathode.remote.action.movies.DismissMovieRecommendation;
import net.simonvt.cathode.remote.action.movies.RateMovie;
import net.simonvt.cathode.remote.action.movies.WatchedMovie;
import net.simonvt.cathode.remote.action.movies.WatchlistMovie;
import net.simonvt.cathode.util.DateUtils;

public class MovieTaskScheduler extends BaseTaskScheduler {

  public MovieTaskScheduler(Context context) {
    super(context);
  }

  /**
   * Add episodes watched outside of trakt to user library.
   *
   * @param movieId The database id of the episode.
   * @param watched Whether the episode has been watched.
   */
  public void setWatched(final long movieId, final boolean watched) {
    execute(new Runnable() {
      @Override public void run() {
        String watchedAt = null;
        long watchedAtMillis = 0L;
        if (watched) {
          watchedAt = TimeUtils.getIsoTime();
          watchedAtMillis = TimeUtils.getMillis(watchedAt);
        }

        final long traktId = MovieWrapper.getTraktId(context.getContentResolver(), movieId);

        MovieWrapper.setWatched(context.getContentResolver(), movieId, watched, watchedAtMillis);

        queue(new WatchedMovie(traktId, watched, watchedAt));
      }
    });
  }

  public void setIsInWatchlist(final long movieId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override public void run() {
        String listedAt = null;
        long listedAtMillis = 0L;
        if (inWatchlist) {
          listedAt = TimeUtils.getIsoTime();
          listedAtMillis = TimeUtils.getMillis(listedAt);
        }

        final long traktId = MovieWrapper.getTraktId(context.getContentResolver(), movieId);

        MovieWrapper.setIsInWatchlist(context.getContentResolver(), movieId, inWatchlist,
            listedAtMillis);

        queue(new WatchlistMovie(traktId, inWatchlist, listedAt));
      }
    });
  }

  public void setIsInCollection(final long movieId, final boolean inCollection) {
    execute(new Runnable() {
      @Override public void run() {
        String collectedAt = null;
        long collectedAtMillis = 0L;
        if (inCollection) {
          collectedAt = TimeUtils.getIsoTime();
          collectedAtMillis = TimeUtils.getMillis(collectedAt);
        }

        final long traktId = MovieWrapper.getTraktId(context.getContentResolver(), movieId);

        MovieWrapper.setIsInCollection(context.getContentResolver(), movieId, inCollection,
            collectedAtMillis);
        queue(new CollectMovie(traktId, inCollection, collectedAt));
      }
    });
  }

  /**
   * Check into a movie on trakt. Think of this method as in between a seen and a scrobble.
   * After checking in, the trakt will automatically display it as watching then switch over to
   * watched status once
   * the duration has elapsed.
   *
   * @param movieId The database id of the movie.
   */
  public void checkin(final long movieId, final String message, final boolean facebook,
      final boolean twitter, final boolean tumblr) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Movies.WATCHING, null, null, null, null);

        if (c.getCount() == 0) {
          c.close();

          Cursor movie = context.getContentResolver().query(Movies.withId(movieId), new String[] {
              MovieColumns.ID, MovieColumns.RUNTIME,
          }, null, null, null);
          movie.moveToFirst();
          final int runtime = movie.getInt(movie.getColumnIndex(MovieColumns.RUNTIME));
          movie.close();

          final long startedAt = System.currentTimeMillis();
          final long expiresAt = startedAt + runtime * DateUtils.MINUTE_IN_MILLIS;

          ContentValues cv = new ContentValues();
          cv.put(MovieColumns.CHECKED_IN, true);
          cv.put(MovieColumns.STARTED_AT, startedAt);
          cv.put(MovieColumns.EXPIRES_AT, expiresAt);
          context.getContentResolver().update(Movies.withId(movieId), cv, null, null);

          final long traktId = MovieWrapper.getTraktId(context.getContentResolver(), movieId);
          queue(new CheckInMovie(traktId, message, facebook, twitter, tumblr));
        }
      }
    });
  }

  /**
   * Notify trakt that user wants to cancel their current check in.
   */
  public void cancelCheckin() {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Movies.WATCHING, new String[] {
            MovieColumns.TMDB_ID,
        }, null, null, null);

        if (c.moveToFirst()) {
          ContentValues cv = new ContentValues();
          cv.put(MovieColumns.CHECKED_IN, false);
          context.getContentResolver().update(Movies.WATCHING, cv, null, null);

          queue(new CancelCheckin());
        }

        c.close();
      }
    });
  }

  public void dismissRecommendation(final long movieId) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = MovieWrapper.getTraktId(context.getContentResolver(), movieId);
        ContentValues cv = new ContentValues();
        cv.put(MovieColumns.RECOMMENDATION_INDEX, -1);
        context.getContentResolver().update(Movies.withId(movieId), cv, null, null);
        queue(new DismissMovieRecommendation(traktId));
      }
    });
  }

  /**
   * Rate a movie on trakt. Depending on the user settings, this will also send out social updates
   * to facebook,
   * twitter, and tumblr.
   *
   * @param movieId The database id of the movie.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  public void rate(final long movieId, final int rating) {
    execute(new Runnable() {
      @Override public void run() {
        String ratedAt = TimeUtils.getIsoTime();
        long ratedAtMillis = TimeUtils.getMillis(ratedAt);

        final long traktId = MovieWrapper.getTraktId(context.getContentResolver(), movieId);

        ContentValues cv = new ContentValues();
        cv.put(MovieColumns.USER_RATING, rating);
        cv.put(MovieColumns.RATED_AT, ratedAtMillis);
        context.getContentResolver().update(Movies.withId(movieId), cv, null, null);

        queue(new RateMovie(traktId, rating, ratedAt));
      }
    });
  }
}
