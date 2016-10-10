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
import javax.inject.Inject;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.HiddenColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.action.CancelCheckin;
import net.simonvt.cathode.remote.action.movies.CheckInMovie;
import net.simonvt.cathode.remote.action.movies.CollectMovie;
import net.simonvt.cathode.remote.action.movies.DismissMovieRecommendation;
import net.simonvt.cathode.remote.action.movies.RateMovie;
import net.simonvt.cathode.remote.action.movies.WatchedMovie;
import net.simonvt.cathode.remote.action.movies.WatchlistMovie;
import net.simonvt.cathode.remote.sync.comments.SyncComments;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.movies.SyncMovieCredits;
import net.simonvt.cathode.remote.sync.movies.SyncRelatedMovies;
import net.simonvt.cathode.tmdb.api.movie.SyncMovieImages;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.schematic.Cursors;

public class MovieTaskScheduler extends BaseTaskScheduler {

  @Inject MovieDatabaseHelper movieHelper;

  public MovieTaskScheduler(Context context) {
    super(context);
  }

  public void sync(final long movieId) {
    sync(movieId, null);
  }

  public void sync(final long movieId, final Job.OnDoneListener onDoneListener) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = movieHelper.getTraktId(movieId);
        final int tmdbId = movieHelper.getTmdbId(movieId);
        queue(new SyncMovie(traktId));
        queue(new SyncMovieImages(tmdbId));
        queue(new SyncMovieCredits(traktId));
        queue(new SyncRelatedMovies(traktId));

        Job job = new SyncComments(ItemType.MOVIE, traktId);
        job.registerOnDoneListener(onDoneListener);
        queue(job);
      }
    });
  }

  public void syncComments(final long movieId) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = movieHelper.getTraktId(movieId);
        queue(new SyncComments(ItemType.MOVIE, traktId));

        ContentValues values = new ContentValues();
        values.put(MovieColumns.LAST_COMMENT_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);
      }
    });
  }

  public void syncRelated(final long movieId, final Job.OnDoneListener onDoneListener) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = movieHelper.getTraktId(movieId);
        Job job = new SyncRelatedMovies(traktId);
        job.registerOnDoneListener(onDoneListener);
        queue(job);

        ContentValues values = new ContentValues();
        values.put(MovieColumns.LAST_RELATED_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);
      }
    });
  }

  public void syncCredits(final long movieId, final Job.OnDoneListener onDoneListener) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = movieHelper.getTraktId(movieId);
        Job job = new SyncMovieCredits(traktId);
        job.registerOnDoneListener(onDoneListener);
        queue(job);

        ContentValues values = new ContentValues();
        values.put(MovieColumns.LAST_CREDITS_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);
      }
    });
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

        final long traktId = movieHelper.getTraktId(movieId);

        movieHelper.setWatched(movieId, watched, watchedAtMillis);

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

        final long traktId = movieHelper.getTraktId(movieId);

        movieHelper.setIsInWatchlist(movieId, inWatchlist, listedAtMillis);

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

        final long traktId = movieHelper.getTraktId(movieId);

        movieHelper.setIsInCollection(movieId, inCollection, collectedAtMillis);
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
        Cursor c = context.getContentResolver().query(Movies.WATCHING, new String[] {
            MovieColumns.ID, MovieColumns.EXPIRES_AT,
        }, null, null, null);

        final long currentTime = System.currentTimeMillis();
        long expires = 0;
        if (c.moveToFirst()) {
          expires = Cursors.getLong(c, MovieColumns.EXPIRES_AT);
        }

        if (c.getCount() == 0 || (expires >= currentTime && expires > 0)) {
          Cursor movie = context.getContentResolver().query(Movies.withId(movieId), new String[] {
              MovieColumns.ID, MovieColumns.RUNTIME,
          }, null, null, null);
          movie.moveToFirst();
          final int runtime = Cursors.getInt(movie, MovieColumns.RUNTIME);
          movie.close();

          final long startedAt = System.currentTimeMillis();
          final long expiresAt = startedAt + runtime * DateUtils.MINUTE_IN_MILLIS;

          ContentValues cv = new ContentValues();
          cv.put(MovieColumns.CHECKED_IN, true);
          cv.put(MovieColumns.STARTED_AT, startedAt);
          cv.put(MovieColumns.EXPIRES_AT, expiresAt);
          context.getContentResolver().update(Movies.withId(movieId), cv, null, null);

          final long traktId = movieHelper.getTraktId(movieId);
          queue(new CheckInMovie(traktId, message, facebook, twitter, tumblr));
        }

        c.close();
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
        final long traktId = movieHelper.getTraktId(movieId);
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

        final long traktId = movieHelper.getTraktId(movieId);

        ContentValues cv = new ContentValues();
        cv.put(MovieColumns.USER_RATING, rating);
        cv.put(MovieColumns.RATED_AT, ratedAtMillis);
        context.getContentResolver().update(Movies.withId(movieId), cv, null, null);

        queue(new RateMovie(traktId, rating, ratedAt));
      }
    });
  }

  public void hideFromCalendar(final long movieId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        // TODO: Wait for trakt support
        // queue(new CalendarHideMovie(movieId, hidden));

        ContentValues values = new ContentValues();
        values.put(HiddenColumns.HIDDEN_CALENDAR, hidden);
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);
      }
    });
  }

  public void hideFromWatched(final long movieId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        // TODO: Wait for trakt support
        // queue(new WatchedHideMovie(movieId, hidden));

        ContentValues values = new ContentValues();
        values.put(HiddenColumns.HIDDEN_WATCHED, hidden);
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);
      }
    });
  }

  public void hideFromCollected(final long movieId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        // TODO: Wait for trakt support
        // queue(new CollectedHideMovie(movieId, hidden));

        ContentValues values = new ContentValues();
        values.put(HiddenColumns.HIDDEN_COLLECTED, hidden);
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);
      }
    });
  }
}
