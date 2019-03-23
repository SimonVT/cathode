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
package net.simonvt.cathode.sync.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.action.shows.AddShowToHistory;
import net.simonvt.cathode.remote.action.shows.CalendarHideShow;
import net.simonvt.cathode.remote.action.shows.CollectedHideShow;
import net.simonvt.cathode.remote.action.shows.DismissShowRecommendation;
import net.simonvt.cathode.remote.action.shows.RateShow;
import net.simonvt.cathode.remote.action.shows.WatchedHideShow;
import net.simonvt.cathode.remote.action.shows.WatchlistShow;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows;
import net.simonvt.cathode.settings.TraktLinkSettings;

@Singleton public class ShowTaskScheduler extends BaseTaskScheduler {

  private EpisodeTaskScheduler episodeScheduler;
  private ShowDatabaseHelper showHelper;

  @Inject public ShowTaskScheduler(Context context, JobManager jobManager,
      EpisodeTaskScheduler episodeScheduler, ShowDatabaseHelper showHelper) {
    super(context, jobManager);
    this.episodeScheduler = episodeScheduler;
    this.showHelper = showHelper;
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
        if (SyncItems.TIME_RELEASED.equals(watchedAt)) {
          showHelper.addToHistory(showId, ShowDatabaseHelper.WATCHED_RELEASE);
        } else {
          showHelper.addToHistory(showId, TimeUtils.getMillis(watchedAt));
        }

        if (TraktLinkSettings.isLinked(context)) {
          final long traktId = showHelper.getTraktId(showId);
          queue(new AddShowToHistory(traktId, watchedAt));
          // No documentation on how exactly the trakt endpoint is implemented, so sync after.
          queue(new SyncWatchedShows());
        }
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

          final int episodeCount = Cursors.getInt(c, ShowColumns.EPISODE_COUNT);
          if (episodeCount == 0) {
            queue(new SyncShow(traktId));
          }

          if (TraktLinkSettings.isLinked(context)) {
            queue(new WatchlistShow(traktId, inWatchlist, listedAt));
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

        ContentValues values = new ContentValues();
        values.put(ShowColumns.RECOMMENDATION_INDEX, -1);
        context.getContentResolver().update(Shows.withId(showId), values, null, null);

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

        ContentValues values = new ContentValues();
        values.put(ShowColumns.USER_RATING, rating);
        values.put(ShowColumns.RATED_AT, ratedAtMillis);
        context.getContentResolver().update(Shows.withId(showId), values, null, null);

        if (TraktLinkSettings.isLinked(context)) {
          final long traktId = showHelper.getTraktId(showId);
          queue(new RateShow(traktId, rating, ratedAt));
        }
      }
    });
  }

  public void hideFromCalendar(final long showId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        ContentValues values = new ContentValues();
        values.put(ShowColumns.HIDDEN_CALENDAR, hidden);
        context.getContentResolver().update(Shows.withId(showId), values, null, null);

        if (TraktLinkSettings.isLinked(context)) {
          final long traktId = showHelper.getTraktId(showId);
          queue(new CalendarHideShow(traktId, hidden));
        }
      }
    });
  }

  public void hideFromWatched(final long showId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        ContentValues values = new ContentValues();
        values.put(ShowColumns.HIDDEN_WATCHED, hidden);
        context.getContentResolver().update(Shows.withId(showId), values, null, null);

        if (TraktLinkSettings.isLinked(context)) {
          final long traktId = showHelper.getTraktId(showId);
          queue(new WatchedHideShow(traktId, hidden));
        }
      }
    });
  }

  public void hideFromCollected(final long showId, final boolean hidden) {
    execute(new Runnable() {
      @Override public void run() {
        ContentValues values = new ContentValues();
        values.put(ShowColumns.HIDDEN_COLLECTED, hidden);
        context.getContentResolver().update(Shows.withId(showId), values, null, null);

        if (TraktLinkSettings.isLinked(context)) {
          final long traktId = showHelper.getTraktId(showId);
          queue(new CollectedHideShow(traktId, hidden));
        }
      }
    });
  }
}
