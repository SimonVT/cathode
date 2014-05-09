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
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.action.CheckInEpisodeTask;
import net.simonvt.cathode.remote.action.EpisodeCollectionTask;
import net.simonvt.cathode.remote.action.EpisodeRateTask;
import net.simonvt.cathode.remote.action.EpisodeWatchedTask;
import net.simonvt.cathode.remote.action.EpisodeWatchlistTask;
import net.simonvt.cathode.remote.sync.SyncEpisodeTask;

public class EpisodeTaskScheduler extends BaseTaskScheduler {

  private static final String TAG = "EpisodeTaskScheduler";

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
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
        final int number = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
        c.close();

        queueTask(new SyncEpisodeTask(tvdbId, season, number));
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
        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.SHOW_ID,
                EpisodeColumns.SEASON, EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
        final int number = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
        c.close();

        EpisodeWrapper.setWatched(context.getContentResolver(), episodeId, watched);

        queuePriorityTask(new EpisodeWatchedTask(tvdbId, season, number, watched));
      }
    });
  }

  public void checkin(final long episodeId, final String message, final boolean facebook,
      final boolean twitter, final boolean tumblr, final boolean path, final boolean prowl) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver()
            .query(Episodes.EPISODES, null, EpisodeColumns.CHECKED_IN + "=1", null, null);

        if (c.getCount() == 0) {
          Cursor episode =
              EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.SHOW_ID,
                  EpisodeColumns.SEASON, EpisodeColumns.EPISODE);
          episode.moveToFirst();
          final long showId = episode.getLong(episode.getColumnIndex(EpisodeColumns.SHOW_ID));
          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
          final int season = episode.getInt(episode.getColumnIndex(EpisodeColumns.SEASON));
          final int number = episode.getInt(episode.getColumnIndex(EpisodeColumns.EPISODE));
          episode.close();

          ContentValues cv = new ContentValues();
          cv.put(EpisodeColumns.CHECKED_IN, true);
          context.getContentResolver().update(Episodes.withId(episodeId), cv, null, null);

          queuePriorityTask(
              new CheckInEpisodeTask(tvdbId, season, number, message, facebook, twitter, tumblr,
                  path, prowl)
          );
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
        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.SHOW_ID,
                EpisodeColumns.SEASON, EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
        final int number = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
        c.close();

        EpisodeWrapper.setInCollection(context.getContentResolver(), episodeId, inCollection);

        queuePriorityTask(new EpisodeCollectionTask(tvdbId, season, number, inCollection));
      }
    });
  }

  public void setIsInWatchlist(final long episodeId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.SHOW_ID,
                EpisodeColumns.SEASON, EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
        final int number = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
        c.close();

        EpisodeWrapper.setIsInWatchlist(context.getContentResolver(), episodeId, inWatchlist);

        queuePriorityTask(new EpisodeWatchlistTask(tvdbId, season, number, inWatchlist));
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
        final int tvdbId = EpisodeWrapper.getShowTvdbId(context.getContentResolver(), episodeId);
        Cursor c =
            EpisodeWrapper.query(context.getContentResolver(), episodeId, EpisodeColumns.EPISODE,
                EpisodeColumns.SEASON);

        if (c.moveToFirst()) {
          final int episode = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
          final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));

          ContentValues cv = new ContentValues();
          cv.put(EpisodeColumns.RATING, rating);
          context.getContentResolver().update(Episodes.withId(episodeId), cv, null, null);

          queue.add(new EpisodeRateTask(tvdbId, season, episode, rating));
        }
        c.close();
      }
    });
  }
}
