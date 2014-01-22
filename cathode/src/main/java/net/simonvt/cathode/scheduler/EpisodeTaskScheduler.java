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
import net.simonvt.cathode.provider.CathodeContract;
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
        Cursor c = EpisodeWrapper.query(context.getContentResolver(), episodeId,
            CathodeContract.Episodes.SHOW_ID, CathodeContract.Episodes.SEASON,
            CathodeContract.Episodes.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(CathodeContract.Episodes.SHOW_ID));
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON));
        final int number = c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE));
        c.close();

        postTask(new SyncEpisodeTask(tvdbId, season, number));
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
        Cursor c = EpisodeWrapper.query(context.getContentResolver(), episodeId,
            CathodeContract.Episodes.SHOW_ID, CathodeContract.Episodes.SEASON,
            CathodeContract.Episodes.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(CathodeContract.Episodes.SHOW_ID));
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON));
        final int number = c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE));
        c.close();

        EpisodeWrapper.setWatched(context.getContentResolver(), episodeId, watched);

        postPriorityTask(new EpisodeWatchedTask(tvdbId, season, number, watched));
      }
    });
  }

  public void checkin(final long episodeId) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(CathodeContract.Episodes.CONTENT_URI, null,
            CathodeContract.Episodes.WATCHING + "=1", null, null);

        if (c.getCount() == 0) {
          Cursor episode = EpisodeWrapper.query(context.getContentResolver(), episodeId,
              CathodeContract.Episodes.SHOW_ID, CathodeContract.Episodes.SEASON,
              CathodeContract.Episodes.EPISODE);
          episode.moveToFirst();
          final long showId =
              episode.getLong(episode.getColumnIndex(CathodeContract.Episodes.SHOW_ID));
          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
          final int season =
              episode.getInt(episode.getColumnIndex(CathodeContract.Episodes.SEASON));
          final int number =
              episode.getInt(episode.getColumnIndex(CathodeContract.Episodes.EPISODE));
          episode.close();

          ContentValues cv = new ContentValues();
          cv.put(CathodeContract.Episodes.WATCHING, true);
          context.getContentResolver()
              .update(CathodeContract.Episodes.buildFromId(episodeId), cv, null, null);

          postPriorityTask(new CheckInEpisodeTask(tvdbId, season, number));
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
        Cursor c = EpisodeWrapper.query(context.getContentResolver(), episodeId,
            CathodeContract.Episodes.SHOW_ID, CathodeContract.Episodes.SEASON,
            CathodeContract.Episodes.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(CathodeContract.Episodes.SHOW_ID));
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON));
        final int number = c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE));
        c.close();

        EpisodeWrapper.setInCollection(context.getContentResolver(), episodeId, inCollection);

        postPriorityTask(new EpisodeCollectionTask(tvdbId, season, number, inCollection));
      }
    });
  }

  public void setIsInWatchlist(final long episodeId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = EpisodeWrapper.query(context.getContentResolver(), episodeId,
            CathodeContract.Episodes.SHOW_ID, CathodeContract.Episodes.SEASON,
            CathodeContract.Episodes.EPISODE);
        c.moveToFirst();
        final long showId = c.getLong(c.getColumnIndex(CathodeContract.Episodes.SHOW_ID));
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        final int season = c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON));
        final int number = c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE));
        c.close();

        EpisodeWrapper.setIsInWatchlist(context.getContentResolver(), episodeId, inWatchlist);

        postPriorityTask(new EpisodeWatchlistTask(tvdbId, season, number, inWatchlist));
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
        Cursor c = EpisodeWrapper.query(context.getContentResolver(), episodeId,
            CathodeContract.Episodes.EPISODE, CathodeContract.Episodes.SEASON);

        if (c.moveToFirst()) {
          final int episode = c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE));
          final int season = c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON));

          ContentValues cv = new ContentValues();
          cv.put(CathodeContract.Episodes.RATING, rating);
          context.getContentResolver()
              .update(CathodeContract.Episodes.buildFromId(episodeId), cv, null, null);

          queue.add(new EpisodeRateTask(tvdbId, season, episode, rating));
        }
        c.close();
      }
    });
  }
}
