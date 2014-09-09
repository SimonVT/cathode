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
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.action.CancelCheckin;
import net.simonvt.cathode.remote.action.shows.DismissShowRecommendation;
import net.simonvt.cathode.remote.action.shows.ShowRateTask;
import net.simonvt.cathode.remote.action.shows.ShowWatchedTask;
import net.simonvt.cathode.remote.action.shows.ShowWatchlistTask;
import net.simonvt.cathode.remote.sync.shows.SyncShowTask;

public class ShowTaskScheduler extends BaseTaskScheduler {

  @Inject EpisodeTaskScheduler episodeScheduler;

  public ShowTaskScheduler(Context context) {
    super(context);
    CathodeApp.inject(context, this);
  }

  /**
   * Sync data for show with Trakt.
   *
   * @param showId The database id of the show.
   */
  public void sync(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.FULL_SYNC_REQUESTED, System.currentTimeMillis());
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);
        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
        queueTask(new SyncShowTask(traktId));
      }
    });
  }

  public void watchedNext(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Episodes.fromShow(showId), new String[] {
                EpisodeColumns.ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
            }, "watched=0 AND season<>0", null,
            EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1");

        if (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndexOrThrow(EpisodeColumns.ID));
          episodeScheduler.setWatched(episodeId, true);
        }

        c.close();
      }
    });
  }

  //public void checkinNext(final long showId) {
  //  execute(new Runnable() {
  //    @Override public void run() {
  //      Cursor c = context.getContentResolver()
  //          .query(CathodeContract.EpisodeColumns.buildFromShowId(showId), new String[] {
  //              CathodeContract.EpisodeColumns.ID, CathodeContract.EpisodeColumns.SEASON,
  //              CathodeContract.EpisodeColumns.EPISODE,
  //          }, "watched=0 AND season<>0", null, CathodeContract.EpisodeColumns.SEASON
  //              + " ASC, "
  //              + CathodeContract.EpisodeColumns.EPISODE
  //              + " ASC LIMIT 1");
  //
  //      if (c.moveToNext()) {
  //        final long episodeId = c.getLong(c.getColumnIndexOrThrow(CathodeContract.EpisodeColumns.ID));
  //        episodeScheduler.checkin(episodeId);
  //      }
  //
  //      c.close();
  //    }
  //  });
  //}

  public void cancelCheckin() {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver()
            .query(Episodes.EPISODES, null, EpisodeColumns.CHECKED_IN + "=1", null, null);
        if (c.moveToNext()) {
          ContentValues cv = new ContentValues();
          cv.put(EpisodeColumns.CHECKED_IN, false);
          context.getContentResolver().update(Episodes.EPISODE_WATCHING, cv, null, null);

          queuePriorityTask(new CancelCheckin());
        }
        c.close();
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
          final long episodeId = c.getLong(c.getColumnIndexOrThrow(EpisodeColumns.ID));
          episodeScheduler.setIsInCollection(episodeId, true);
        }

        c.close();
      }
    });
  }

  public void setWatched(final long showId, final boolean watched) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Shows.withId(showId), new String[] {
            ShowColumns.TVDB_ID,
        }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(ShowColumns.TVDB_ID));
          ShowWrapper.setWatched(context.getContentResolver(), showId, watched);
          queue.add(new ShowWatchedTask(tvdbId, watched));
        }

        c.close();
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

          final long traktId = c.getLong(c.getColumnIndex(ShowColumns.TRAKT_ID));
          ShowWrapper.setIsInWatchlist(context.getContentResolver(), showId, inWatchlist,
              listedAtMillis);
          queue.add(new ShowWatchlistTask(traktId, inWatchlist, listedAt));

          final int episodeCount = c.getInt(c.getColumnIndex(ShowColumns.EPISODE_COUNT));
          if (episodeCount == 0) {
            queueTask(new SyncShowTask(traktId));
          }
        }

        c.close();
      }
    });
  }

  public void setIsHidden(final long showId, final boolean isHidden) {
    execute(new Runnable() {
      @Override public void run() {
        ShowWrapper.setIsHidden(context.getContentResolver(), showId, isHidden);
      }
    });
  }

  public void dismissRecommendation(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);

        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.RECOMMENDATION_INDEX, -1);
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);

        queue.add(new DismissShowRecommendation(traktId));
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

        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);

        ContentValues cv = new ContentValues();
        cv.put(ShowColumns.RATING, rating);
        cv.put(ShowColumns.RATED_AT, ratedAtMillis);
        context.getContentResolver().update(Shows.withId(showId), cv, null, null);

        queue.add(new ShowRateTask(traktId, rating, ratedAt));
      }
    });
  }
}
