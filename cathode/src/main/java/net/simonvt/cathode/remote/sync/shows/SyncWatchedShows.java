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
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit.Call;
import timber.log.Timber;

public class SyncWatchedShows extends CallJob<List<WatchedItem>> {

  @Inject transient SyncService syncService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

  public SyncWatchedShows() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncWatchedShows";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public Call<List<WatchedItem>> getCall() {
    return syncService.getWatchedShows();
  }

  @Override public void handleResponse(List<WatchedItem> watched) {
    try {
      ContentResolver resolver = getContentResolver();

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
        ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
        final long showId = showResult.showId;
        final boolean didShowExist = !showResult.didCreate;
        if (showResult.didCreate) {
          queue(new SyncShow(traktId));
        }

        long lastWatchedMillis = 0;
        IsoTime lastWatched = item.getLastWatchedAt();
        if (lastWatched != null) {
          // TODO: Find out if this can happen once released
          lastWatchedMillis = lastWatched.getTimeInMillis();
        } else {
          Timber.e(new IllegalStateException("lastWatched is null"), "lastWatched is null");
        }

        ops.add(ContentProviderOperation.newUpdate(ProviderSchematic.Shows.withId(showId))
            .withValue(DatabaseContract.ShowColumns.LAST_WATCHED_AT, lastWatchedMillis)
            .build());

        List<WatchedItem.Season> seasons = item.getSeasons();
        for (WatchedItem.Season season : seasons) {
          final int seasonNumber = season.getNumber();
          SeasonDatabaseHelper.IdResult seasonResult =
              seasonHelper.getIdOrCreate(showId, seasonNumber);
          final long seasonId = seasonResult.id;
          final boolean didSeasonExist = !seasonResult.didCreate;
          if (seasonResult.didCreate) {
            if (didShowExist) {
              queue(new SyncShow(traktId, true));
            }
          }

          List<WatchedItem.Episode> episodes = season.getEpisodes();
          for (WatchedItem.Episode episode : episodes) {
            final int episodeNumber = episode.getNumber();
            EpisodeDatabaseHelper.IdResult episodeResult =
                episodeHelper.getIdOrCreate(showId, seasonId, episodeNumber);
            final long episodeId = episodeResult.id;
            if (episodeResult.didCreate) {
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
