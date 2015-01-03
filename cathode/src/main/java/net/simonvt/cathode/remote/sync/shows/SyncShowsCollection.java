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
import net.simonvt.cathode.api.entity.CollectionItem;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobFailedException;
import timber.log.Timber;

public class SyncShowsCollection extends Job {

  @Inject transient SyncService syncService;

  @Override public String key() {
    return "SyncShowsCollection";
  }

  @Override public int getPriority() {
    return PRIORITY_4;
  }

  @Override public void perform() {
    try {
      ContentResolver resolver = getContentResolver();
      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

      List<CollectionItem> collection = syncService.getShowCollection();

      Cursor c = resolver.query(Episodes.EPISODES, new String[] {
          EpisodeColumns.ID, EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON_ID,
      }, EpisodeColumns.IN_COLLECTION + "=1", null, null);

      List<Long> episodeIds = new ArrayList<Long>(c.getCount());
      while (c.moveToNext()) {
        episodeIds.add(c.getLong(0));
      }
      c.close();

      for (CollectionItem item : collection) {
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

        ops.add(ContentProviderOperation.newUpdate(Shows.withId(showId))
            .withValue(ShowColumns.LAST_COLLECTED_AT, item.getLastCollectedAt().getTimeInMillis())
            .build());

        List<CollectionItem.Season> seasons = item.getSeasons();
        for (CollectionItem.Season season : seasons) {
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

          List<CollectionItem.Episode> episodes = season.getEpisodes();
          for (CollectionItem.Episode episode : episodes) {
            final int episodeNumber = episode.getNumber();
            long episodeId =
                EpisodeWrapper.getEpisodeId(resolver, showId, seasonNumber, episodeNumber);

            if (episodeId == -1L) {
              episodeId = EpisodeWrapper.createEpisode(resolver, showId, seasonId, episodeNumber);
              if (didSeasonExist) {
                queue(new SyncEpisode(traktId, seasonNumber, episodeNumber));
              }
            }

            if (!episodeIds.remove(episodeId)) {
              ContentProviderOperation.Builder builder =
                  ContentProviderOperation.newUpdate(Episodes.withId(episodeId));
              ContentValues cv = new ContentValues();
              cv.put(EpisodeColumns.IN_COLLECTION, true);
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
        builder.withValue(EpisodeColumns.IN_COLLECTION, false);
        if (first) {
          builder.withYieldAllowed(true);
          first = false;
        }
        ops.add(builder.build());
      }

      resolver.applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "SyncShowsCollectionTask failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncShowsCollectionTask failed");
      throw new JobFailedException(e);
    }
  }
}
