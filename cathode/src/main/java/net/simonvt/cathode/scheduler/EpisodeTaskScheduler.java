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
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.action.shows.CheckInEpisodeTask;
import net.simonvt.cathode.remote.action.shows.EpisodeCollectionTask;
import net.simonvt.cathode.remote.action.shows.EpisodeRateTask;
import net.simonvt.cathode.remote.action.shows.EpisodeWatchedTask;
import net.simonvt.cathode.remote.action.shows.EpisodeWatchlistTask;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeTask;

public class EpisodeTaskScheduler extends BaseTaskScheduler {

  public EpisodeTaskScheduler(Context context) {
    super(context);
  }

  /**
   * Sync data for episode with Trakt.
   *
   * @param episodeId The database id of the episode.
   */
  public void sync(final long episodeId) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.SHOW_ID,
                EpisodeColumns.SEASON, EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
        final int number = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
        c.close();

        queueTask(new SyncEpisodeTask(traktId, season, number));
      }
    });
  }

  /**
   * Add episodes watched outside of trakt to user library.
   *
   * @param episodeId The database id of the episode.
   * @param watched Whether the episode has been watched.
   */
  public void setWatched(final long episodeId, final boolean watched) {
    execute(new Runnable() {
      @Override public void run() {
        String watchedAt = null;
        long watchedAtMillis = 0L;
        if (watched) {
          watchedAt = TimeUtils.getIsoTime();
          watchedAtMillis = TimeUtils.getMillis(watchedAt);
        }

        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.SHOW_ID,
                EpisodeColumns.SEASON, EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
        final int number = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
        c.close();

        EpisodeWrapper.setWatched(context.getContentResolver(), showId, episodeId, watched,
            watchedAtMillis);

        queuePriorityTask(new EpisodeWatchedTask(traktId, season, number, watched, watchedAt));
      }
    });
  }

  public void checkin(final long episodeId, final String message, final boolean facebook,
      final boolean twitter, final boolean tumblr) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver()
            .query(Episodes.EPISODES, null, EpisodeColumns.CHECKED_IN + "=1", null, null);

        if (c.getCount() == 0) {
          Cursor episode = EpisodeWrapper.query(context.getContentResolver(), episodeId,
              EpisodeColumns.TRAKT_ID);
          episode.moveToFirst();
          final long traktId = episode.getLong(episode.getColumnIndex(EpisodeColumns.TRAKT_ID));
          episode.close();

          ContentValues cv = new ContentValues();
          cv.put(EpisodeColumns.CHECKED_IN, true);
          context.getContentResolver().update(Episodes.withId(episodeId), cv, null, null);

          queuePriorityTask(
              new CheckInEpisodeTask(traktId, message, facebook, twitter, tumblr));
        }
      }
    });
  }

  /**
   * Add episodes to user library collection.
   *
   * @param episodeId The database id of the episode.
   */
  public void setIsInCollection(final long episodeId, final boolean inCollection) {
    execute(new Runnable() {
      @Override public void run() {
        String collectedAt = null;
        long collectedAtMillis = 0L;
        if (inCollection) {
          collectedAt = TimeUtils.getIsoTime();
          collectedAtMillis = TimeUtils.getMillis(collectedAt);
        }

        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.SHOW_ID,
                EpisodeColumns.SEASON, EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
        final int number = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
        c.close();

        EpisodeWrapper.setInCollection(context.getContentResolver(), episodeId, inCollection,
            collectedAtMillis);

        queuePriorityTask(
            new EpisodeCollectionTask(traktId, season, number, inCollection, collectedAt));
      }
    });
  }

  public void setIsInWatchlist(final long episodeId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override public void run() {
        String listedAt = null;
        long listeddAtMillis = 0L;
        if (inWatchlist) {
          listedAt = TimeUtils.getIsoTime();
          listeddAtMillis = TimeUtils.getMillis(listedAt);
        }

        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.SHOW_ID,
                EpisodeColumns.SEASON, EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
        final long traktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
        final int number = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
        c.close();

        EpisodeWrapper.setIsInWatchlist(context.getContentResolver(), episodeId, inWatchlist,
            listeddAtMillis);

        queuePriorityTask(new EpisodeWatchlistTask(traktId, season, number, inWatchlist, listedAt));
      }
    });
  }

  /**
   * Rate an episode on trakt. Depending on the user settings, this will also send out social
   * updates to facebook,
   * twitter, and tumblr.
   *
   * @param episodeId The database id of the episode.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  public void rate(final long episodeId, final int rating) {
    execute(new Runnable() {
      @Override public void run() {
        String ratedAt = TimeUtils.getIsoTime();
        long ratedAtMillis = TimeUtils.getMillis(ratedAt);

        final long traktId = EpisodeWrapper.getShowTraktId(context.getContentResolver(), episodeId);
        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.EPISODE,
                EpisodeColumns.SEASON);

        if (c.moveToFirst()) {
          final int episode = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
          final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));

          ContentValues cv = new ContentValues();
          cv.put(EpisodeColumns.RATING, rating);
          cv.put(EpisodeColumns.RATED_AT, ratedAtMillis);
          context.getContentResolver().update(Episodes.withId(episodeId), cv, null, null);

          queue.add(new EpisodeRateTask(traktId, season, episode, rating, ratedAt));
        }
        c.close();
      }
    });
  }
}
