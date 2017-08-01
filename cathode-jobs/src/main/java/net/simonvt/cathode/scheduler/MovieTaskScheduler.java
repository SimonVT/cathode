/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
import android.text.format.DateUtils;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.common.event.ErrorEvent;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobs.R;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.action.RemoveHistoryItem;
import net.simonvt.cathode.remote.action.movies.AddMovieToHistory;
import net.simonvt.cathode.remote.action.movies.CalendarHideMovie;
import net.simonvt.cathode.remote.action.movies.CollectMovie;
import net.simonvt.cathode.remote.action.movies.DismissMovieRecommendation;
import net.simonvt.cathode.remote.action.movies.RateMovie;
import net.simonvt.cathode.remote.action.movies.RemoveMovieFromHistory;
import net.simonvt.cathode.remote.action.movies.WatchlistMovie;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.comments.SyncComments;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.movies.SyncMovieCredits;
import net.simonvt.cathode.remote.sync.movies.SyncRelatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncWatchedMovies;
import net.simonvt.cathode.tmdb.api.movie.SyncMovieImages;
import net.simonvt.cathode.trakt.CheckIn;
import net.simonvt.schematic.Cursors;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class MovieTaskScheduler extends BaseTaskScheduler {

  @Inject MovieDatabaseHelper movieHelper;
  @Inject CheckinService checkinService;
  @Inject CheckIn checkIn;

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

  public void addToHistoryNow(final long movieId) {
    addToHistory(movieId, System.currentTimeMillis());
  }

  public void addToHistoryOnRelease(final long movieId) {
    addToHistory(movieId, SyncItems.TIME_RELEASED);
  }

  public void addToHistory(final long movieId, final long watchedAt) {
    final String isoWhen = TimeUtils.getIsoTime(watchedAt);
    addToHistory(movieId, isoWhen);
  }

  public void addToHistory(final long movieId, final int year, final int month, final int day,
      final int hour, final int minute) {
    addToHistory(movieId, TimeUtils.getMillis(year, month, day, hour, minute));
  }

  /**
   * Add episodes watched outside of trakt to user library.
   *
   * @param movieId The database id of the episode.
   */
  public void addToHistory(final long movieId, final String watchedAt) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = movieHelper.getTraktId(movieId);

        if (SyncItems.TIME_RELEASED.equals(watchedAt)) {
          movieHelper.addToHistory(movieId, MovieDatabaseHelper.WATCHED_RELEASE);
        } else {
          movieHelper.addToHistory(movieId, TimeUtils.getMillis(watchedAt));
        }

        queue(new AddMovieToHistory(traktId, watchedAt));
      }
    });
  }

  public void removeFromHistory(final long movieId) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = movieHelper.getTraktId(movieId);
        movieHelper.removeFromHistory(movieId);
        queue(new RemoveMovieFromHistory(traktId));
      }
    });
  }

  public void removeHistoryItem(final long movieId, final long historyId, final boolean lastItem) {
    execute(new Runnable() {
      @Override public void run() {
        queue(new RemoveHistoryItem(historyId));

        if (lastItem) {
          movieHelper.removeFromHistory(movieId);
        }

        queue(new SyncWatchedMovies());
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
        Cursor watching = context.getContentResolver().query(Movies.WATCHING, new String[] {
            MovieColumns.RUNTIME, MovieColumns.EXPIRES_AT,
        }, null, null, null);

        watching.moveToFirst();
        final long currentTime = System.currentTimeMillis();
        final int runtime = Cursors.getInt(watching, MovieColumns.RUNTIME);
        final long expires = Cursors.getLong(watching, MovieColumns.EXPIRES_AT);
        final long watchSlop = (long) (runtime * DateUtils.MINUTE_IN_MILLIS * 0.8f);

        Cursor movie = context.getContentResolver().query(Movies.withId(movieId), new String[] {
            MovieColumns.TITLE,
        }, null, null, null);
        movie.moveToFirst();
        final String title = Cursors.getString(movie, MovieColumns.TITLE);
        movie.close();

        if (watching.getCount() == 0 || ((expires - watchSlop) < currentTime && expires > 0)) {
          if (checkIn.movie(movieId, message, facebook, twitter, tumblr)) {
            movieHelper.checkIn(movieId);
          }
        } else {
          ErrorEvent.post(context.getString(R.string.checkin_error_watching, title));
        }

        watching.close();
        queue(new SyncWatching());
      }
    });
  }

  /**
   * Notify trakt that user wants to cancel their current check in.
   */
  public void cancelCheckin() {
    execute(new Runnable() {
      @Override public void run() {
        Cursor movie = null;
        try {
          movie = context.getContentResolver().query(Movies.WATCHING, new String[] {
              MovieColumns.ID, MovieColumns.STARTED_AT, MovieColumns.EXPIRES_AT,
          }, null, null, null);

          if (movie.moveToFirst()) {
            final long id = Cursors.getLong(movie, MovieColumns.ID);
            final long startedAt = Cursors.getLong(movie, MovieColumns.STARTED_AT);
            final long expiresAt = Cursors.getLong(movie, MovieColumns.EXPIRES_AT);

            ContentValues values = new ContentValues();
            values.put(MovieColumns.CHECKED_IN, false);
            context.getContentResolver().update(Movies.WATCHING, values, null, null);

            try {
              Call<ResponseBody> call = checkinService.deleteCheckin();
              Response<ResponseBody> response = call.execute();
              if (response.isSuccessful()) {
                return;
              }
            } catch (IOException e) {
              Timber.d(e);
            }

            ErrorEvent.post(context.getString(R.string.checkin_cancel_error));

            values.clear();
            values.put(MovieColumns.CHECKED_IN, true);
            values.put(MovieColumns.STARTED_AT, startedAt);
            values.put(MovieColumns.EXPIRES_AT, expiresAt);
            context.getContentResolver().update(Movies.withId(id), values, null, null);

            queue(new SyncWatching());
          }
        } finally {
          if (movie != null) {
            movie.close();
          }
        }
      }
    });
  }

  public void dismissRecommendation(final long movieId) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = movieHelper.getTraktId(movieId);
        ContentValues values = new ContentValues();
        values.put(MovieColumns.RECOMMENDATION_INDEX, -1);
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);
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

        ContentValues values = new ContentValues();
        values.put(MovieColumns.USER_RATING, rating);
        values.put(MovieColumns.RATED_AT, ratedAtMillis);
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);

        queue(new RateMovie(traktId, rating, ratedAt));
      }
    });
  }

  public void hideFromCalendar(final long movieId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = movieHelper.getTraktId(movieId);
        queue(new CalendarHideMovie(traktId, hidden));

        ContentValues values = new ContentValues();
        values.put(MovieColumns.HIDDEN_CALENDAR, hidden);
        context.getContentResolver().update(Movies.withId(movieId), values, null, null);
      }
    });
  }
}
