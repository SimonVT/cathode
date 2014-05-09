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
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.remote.action.EpisodeCollectionTask;
import net.simonvt.cathode.remote.action.EpisodeWatchedTask;

public class SeasonTaskScheduler extends BaseTaskScheduler {

  @Inject EpisodeTaskScheduler episodeScheduler;

  public SeasonTaskScheduler(Context context) {
    super(context);
  }

  public void setWatched(final long seasonId, final boolean watched) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Episodes.fromSeason(seasonId), new String[] {
            EpisodeColumns.ID, EpisodeColumns.WATCHED, EpisodeColumns.SEASON,
            EpisodeColumns.EPISODE,
        }, null, null, null);

        ContentValues cv = new ContentValues();
        cv.put(EpisodeColumns.WATCHED, watched);
        context.getContentResolver().update(Episodes.fromSeason(seasonId), cv, null, null);

        while (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndex(EpisodeColumns.ID));
          final boolean episodeWatched = c.getLong(c.getColumnIndex(EpisodeColumns.WATCHED)) != 0;
          final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
          final int episode = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
          final int tvdbId = EpisodeWrapper.getShowTvdbId(context.getContentResolver(), episodeId);

          if (watched != episodeWatched) {
            queuePriorityTask(new EpisodeWatchedTask(tvdbId, season, episode, watched));
          }
        }

        c.close();
      }
    });
  }

  public void setInCollection(final long seasonId, final boolean inCollection) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = context.getContentResolver().query(Episodes.fromSeason(seasonId), new String[] {
            EpisodeColumns.ID, EpisodeColumns.IN_COLLECTION, EpisodeColumns.SEASON,
            EpisodeColumns.EPISODE,
        }, null, null, null);

        ContentValues cv = new ContentValues();
        cv.put(EpisodeColumns.IN_COLLECTION, inCollection);
        context.getContentResolver().update(Episodes.fromSeason(seasonId), cv, null, null);

        while (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndex(EpisodeColumns.ID));
          final boolean episodeInCollection =
              c.getLong(c.getColumnIndex(EpisodeColumns.IN_COLLECTION)) != 0;
          final int season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
          final int episode = c.getInt(c.getColumnIndex(EpisodeColumns.EPISODE));
          final int tvdbId = EpisodeWrapper.getShowTvdbId(context.getContentResolver(), episodeId);

          if (inCollection != episodeInCollection) {
            queuePriorityTask(new EpisodeCollectionTask(tvdbId, season, episode, inCollection));
          }
        }

        c.close();
      }
    });
  }
}
