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
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.action.shows.AddShowToHistory;
import net.simonvt.cathode.remote.action.shows.CalendarHideShow;
import net.simonvt.cathode.remote.action.shows.CollectedHideShow;
import net.simonvt.cathode.remote.action.shows.DismissShowRecommendation;
import net.simonvt.cathode.remote.action.shows.RateShow;
import net.simonvt.cathode.remote.action.shows.WatchedHideShow;
import net.simonvt.cathode.remote.action.shows.WatchlistShow;
import net.simonvt.cathode.remote.sync.comments.SyncComments;
import net.simonvt.cathode.remote.sync.shows.SyncRelatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.cathode.remote.sync.shows.SyncShowCollectedStatus;
import net.simonvt.cathode.remote.sync.shows.SyncShowCredits;
import net.simonvt.cathode.remote.sync.shows.SyncShowWatchedStatus;
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows;
import net.simonvt.cathode.tmdb.api.show.SyncShowImages;
import net.simonvt.schematic.Cursors;

public class ShowTaskScheduler extends BaseTaskScheduler {

  @Inject EpisodeTaskScheduler episodeScheduler;

  @Inject ShowDatabaseHelper showHelper;

  public ShowTaskScheduler(Context context) {
    super(context);
    Injector.obtain().inject(this);
  }

  public void sync(final long showId) {
    sync(showId, null);
  }

  public void sync(final long showId, final Job.OnDoneListener onDoneListener) {
    execute(new Runnable() {
      @Override public void run() {
        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.FULL_SYNC_REQUESTED, System.currentTimeMillis());
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);
        final long traktId = showHelper.getTraktId(showId);
        final int tmdbId = showHelper.getTmdbId(showId);
        queue(new SyncShow(traktId));
        queue(new SyncShowImages(tmdbId));
        queue(new SyncComments(ItemType.SHOW, traktId));
        queue(new SyncShowWatchedStatus(traktId));
        queue(new SyncShowCredits(traktId));
        queue(new SyncRelatedShows(traktId));

        Job job = new SyncShowCollectedStatus(traktId);
        job.registerOnDoneListener(onDoneListener);
        queue(job);
      }
    });
  }

  public void syncComments(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);
        queue(new SyncComments(ItemType.SHOW, traktId));

        ContentValues values = new ContentValues();
        values.put(ShowColumns.LAST_COMMENT_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(Shows.withId(showId), values, null, null);
      }
    });
  }

  public void syncRelated(final long showId, final Job.OnDoneListener onDoneListener) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);
        Job job = new SyncRelatedShows(traktId);
        job.registerOnDoneListener(onDoneListener);
        queue(job);

        ContentValues values = new ContentValues();
        values.put(ShowColumns.LAST_RELATED_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(Shows.withId(showId), values, null, null);
      }
    });
  }

  public void syncCredits(final long showId, final Job.OnDoneListener onDoneListener) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);
        Job job = new SyncShowCredits(traktId);
        job.registerOnDoneListener(onDoneListener);
        queue(job);

        ContentValues values = new ContentValues();
        values.put(ShowColumns.LAST_CREDITS_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(Shows.withId(showId), values, null, null);
      }
    });
  }

  public void watchedNext(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        final long episodeId = showHelper.getNextEpisodeId(showId);
        if (episodeId != -1L) {
          episodeScheduler.addToHistory(episodeId, System.currentTimeMillis());
        }
      }
    });
  }

  public void collectedNext(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Episodes.fromShow(showId), new String[] {
                EpisodeColumns.ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
            }, "inCollection=0 AND season<>0", null,
            EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1");

        if (c.moveToNext()) {
          final long episodeId = Cursors.getLong(c, EpisodeColumns.ID);
          episodeScheduler.setIsInCollection(episodeId, true);
        }

        c.close();
      }
    });
  }

  public void addToHistoryNow(final long showId) {
    addToHistory(showId, System.currentTimeMillis());
  }

  public void addToHistoryOnRelease(final long showId) {
    addToHistory(showId, SyncItems.TIME_RELEASED);
  }

  public void addToHistory(final long showId, final long watchedAt) {
    final String isoWhen = TimeUtils.getIsoTime(watchedAt);
    addToHistory(showId, isoWhen);
  }

  public void addToHistory(final long showId, final int year, final int month, final int day,
      final int hour, final int minute) {
    addToHistory(showId, TimeUtils.getMillis(year, month, day, hour, minute));
  }

  public void addToHistory(final long showId, final String watchedAt) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);

        if (SyncItems.TIME_RELEASED.equals(watchedAt)) {
          showHelper.addToHistory(showId, ShowDatabaseHelper.WATCHED_RELEASE);
        } else {
          showHelper.addToHistory(showId, TimeUtils.getMillis(watchedAt));
        }

        queue(new AddShowToHistory(traktId, watchedAt));
        // No documentation on how exactly the trakt endpoint is implemented, so sync after.
        queue(new SyncWatchedShows());
      }
    });
  }

  public void setIsInWatchlist(final long showId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Shows.withId(showId), new String[] {
            ShowColumns.TRAKT_ID, ShowColumns.EPISODE_COUNT,
        }, null, null, null);

        if (c.moveToFirst()) {
          String listedAt = null;
          long listedAtMillis = 0L;
          if (inWatchlist) {
            listedAt = TimeUtils.getIsoTime();
            listedAtMillis = TimeUtils.getMillis(listedAt);
          }

          final long traktId = Cursors.getLong(c, ShowColumns.TRAKT_ID);
          showHelper.setIsInWatchlist(showId, inWatchlist, listedAtMillis);
          queue(new WatchlistShow(traktId, inWatchlist, listedAt));

          final int episodeCount = Cursors.getInt(c, ShowColumns.EPISODE_COUNT);
          if (episodeCount == 0) {
            queue(new SyncShow(traktId));
          }
        }

        c.close();
      }
    });
  }

  public void dismissRecommendation(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);

        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.RECOMMENDATION_INDEX, -1);
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);

        queue(new DismissShowRecommendation(traktId));
      }
    });
  }

  /**
   * Rate a show on trakt. Depending on the user settings, this will also send out social updates
   * to facebook,
   * twitter, and tumblr.
   *
   * @param showId The database id of the show.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  public void rate(final long showId, final int rating) {
    execute(new Runnable() {
      @Override public void run() {
        String ratedAt = TimeUtils.getIsoTime();
        long ratedAtMillis = TimeUtils.getMillis(ratedAt);

        final long traktId = showHelper.getTraktId(showId);

        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.USER_RATING, rating);
        cv.put(ShowColumns.RATED_AT, ratedAtMillis);
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);

        queue(new RateShow(traktId, rating, ratedAt));
      }
    });
  }

  public void hideFromCalendar(final long showId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);
        queue(new CalendarHideShow(traktId, hidden));

        ContentValues values = new ContentValues();
        values.put(ShowColumns.HIDDEN_CALENDAR, hidden);
        context.getContentResolver().update(Shows.withId(showId), values, null, null);
      }
    });
  }

  public void hideFromWatched(final long showId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);
        queue(new WatchedHideShow(traktId, hidden));

        ContentValues values = new ContentValues();
        values.put(ShowColumns.HIDDEN_WATCHED, hidden);
        context.getContentResolver().update(Shows.withId(showId), values, null, null);
      }
    });
  }

  public void hideFromCollected(final long showId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);
        queue(new CollectedHideShow(traktId, hidden));

        ContentValues values = new ContentValues();
        values.put(ShowColumns.HIDDEN_COLLECTED, hidden);
        context.getContentResolver().update(Shows.withId(showId), values, null, null);
      }
    });
  }
}
