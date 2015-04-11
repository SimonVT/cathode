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
package net.simonvt.cathode.remote.sync.shows;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.IsoTime;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.entity.WatchedItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobFailedException;
import timber.log.Timber;

public class SyncWatchedShows extends Job {

  @Inject transient SyncService syncService;

  @Override public String key() {
    return "SyncWatchedShows";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public void perform() {
    try {
      ContentResolver resolver = getContentResolver();

      List<WatchedItem> watched = syncService.getWatchedShows();

      Cursor c = resolver.query(Episodes.EPISODES, new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.WATCHED, null, null);

      final int episodeIdIndex = c.getColumnIndex(EpisodeColumns.ID);

      List<Long> episodeIds = new ArrayList<Long>(c.getCount());
      while (c.moveToNext()) {
        episodeIds.add(c.getLong(episodeIdIndex));
      }
      c.close();

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

      for (WatchedItem item : watched) {
        boolean allowYield = true;
        Show show = item.getShow();
        final long traktId = show.getIds().getTrakt();

        boolean didShowExist = true;
        long showId = ShowWrapper.getShowId(resolver, traktId);
        if (showId == -1L) {
          didShowExist = false;
          showId = ShowWrapper.createShow(resolver, traktId);
          queue(new SyncShow(traktId));
        }

        long lastWatchedMillis = 0;
        IsoTime lastWatched = item.getLastWatchedAt();
        if (lastWatched != null) {
          // TODO: Find out if this can happen once released
          lastWatchedMillis = lastWatched.getTimeInMillis();
        }

        ops.add(ContentProviderOperation.newUpdate(ProviderSchematic.Shows.withId(showId))
            .withValue(DatabaseContract.ShowColumns.LAST_WATCHED_AT, lastWatchedMillis)
            .build());

        List<WatchedItem.Season> seasons = item.getSeasons();
        for (WatchedItem.Season season : seasons) {
          final int seasonNumber = season.getNumber();

          boolean didSeasonExist = true;
          long seasonId = SeasonWrapper.getSeasonId(resolver, showId, seasonNumber);
          if (seasonId == -1L) {
            didSeasonExist = false;
            seasonId = SeasonWrapper.createSeason(resolver, showId, seasonNumber);
            if (didShowExist) {
              queue(new SyncSeason(traktId, seasonNumber));
            }
          }

          List<WatchedItem.Episode> episodes = season.getEpisodes();
          for (WatchedItem.Episode episode : episodes) {
            final int episodeNumber = episode.getNumber();
            long episodeId =
                EpisodeWrapper.getEpisodeId(resolver, showId, seasonNumber, episodeNumber);

            if (episodeId == -1L) {
              episodeId = EpisodeWrapper.createEpisode(resolver, showId, seasonId, episodeNumber);
              if (didShowExist && didSeasonExist) {
                queue(new SyncSeason(traktId, seasonNumber));
              }
            }

            if (!episodeIds.remove(episodeId)) {
              ContentProviderOperation.Builder builder =
                  ContentProviderOperation.newUpdate(Episodes.withId(episodeId));
              ContentValues cv = new ContentValues();
              cv.put(EpisodeColumns.WATCHED, true);
              builder.withValues(cv);
              if (allowYield) {
                builder.withYieldAllowed(true);
                allowYield = false;
              }
              ops.add(builder.build());
            }
          }
        }
      }

      boolean first = true;
      for (long episodeId : episodeIds) {
        ContentProviderOperation.Builder builder =
            ContentProviderOperation.newUpdate(Episodes.withId(episodeId));
        ContentValues cv = new ContentValues();
        cv.put(EpisodeColumns.WATCHED, false);
        builder.withValues(cv);
        if (first) {
          builder.withYieldAllowed(true);
          first = false;
        }
        ops.add(builder.build());
      }

      resolver.applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "SyncShowsWatchedTask failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncShowsWatchedTask failed");
      throw new JobFailedException(e);
    }
  }
}
