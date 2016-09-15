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
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SparseArrayCompat;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.CollectionItem;
import net.simonvt.cathode.api.entity.IsoTime;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import timber.log.Timber;

public class SyncShowsCollection extends CallJob<List<CollectionItem>> {

  @Inject transient SyncService syncService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

  public SyncShowsCollection() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncShowsCollection";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public Call<List<CollectionItem>> getCall() {
    return syncService.getShowCollection();
  }

  @Override public void handleResponse(List<CollectionItem> collection) {
    Cursor c = getContentResolver().query(Episodes.EPISODES, new String[] {
        EpisodeColumns.ID, EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON, EpisodeColumns.SEASON_ID,
        EpisodeColumns.EPISODE,
    }, EpisodeColumns.IN_COLLECTION, null, null);

    LongSparseArray<CollectedShow> showsMap = new LongSparseArray<>();
    LongSparseArray<Long> showIdToTraktMap = new LongSparseArray<>();
    List<Long> episodeIds = new ArrayList<>(c.getCount());

    while (c.moveToNext()) {
      final long id = Cursors.getLong(c, EpisodeColumns.ID);
      final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
      final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
      final long seasonId = Cursors.getLong(c, EpisodeColumns.SEASON_ID);

      CollectedShow collectedShow;
      Long showTraktId = showIdToTraktMap.get(showId);
      if (showTraktId == null) {
        showTraktId = showHelper.getTraktId(showId);

        showIdToTraktMap.put(showId, showTraktId);

        collectedShow = new CollectedShow(showTraktId, showId);
        showsMap.put(showTraktId, collectedShow);
      } else {
        collectedShow = showsMap.get(showTraktId);
      }

      CollectedSeason syncSeason = collectedShow.seasons.get(season);
      if (syncSeason == null) {
        syncSeason = new CollectedSeason(season, seasonId);
        collectedShow.seasons.put(season, syncSeason);
      }

      final int number = Cursors.getInt(c, EpisodeColumns.EPISODE);

      CollectedEpisode syncEpisode = syncSeason.episodes.get(number);
      if (syncEpisode == null) {
        syncEpisode = new CollectedEpisode(id, number);
        syncSeason.episodes.put(number, syncEpisode);
      }

      episodeIds.add(id);
    }
    c.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (CollectionItem item : collection) {
      Show show = item.getShow();
      final long traktId = show.getIds().getTrakt();

      CollectedShow collectedShow = showsMap.get(traktId);

      boolean didShowExist = true;
      if (collectedShow == null) {
        ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
        final long showId = showResult.showId;
        didShowExist = !showResult.didCreate;

        if (showHelper.needsSync(showId)) {
          queue(new SyncShow(traktId));
        }

        collectedShow = new CollectedShow(traktId, showId);
        showsMap.put(traktId, collectedShow);
      }

      IsoTime lastCollected = item.getLastCollectedAt();
      final long lastCollectedMillis = lastCollected.getTimeInMillis();

      ops.add(ContentProviderOperation.newUpdate(Shows.withId(collectedShow.id))
          .withValue(ShowColumns.LAST_COLLECTED_AT, lastCollectedMillis)
          .build());

      List<CollectionItem.Season> seasons = item.getSeasons();
      for (CollectionItem.Season season : seasons) {
        final int seasonNumber = season.getNumber();
        CollectedSeason collectedSeason = collectedShow.seasons.get(seasonNumber);
        boolean didSeasonExist = true;
        if (collectedSeason == null) {
          SeasonDatabaseHelper.IdResult seasonResult =
              seasonHelper.getIdOrCreate(collectedShow.id, seasonNumber);
          final long seasonId = seasonResult.id;
          if (seasonResult.didCreate) {
            didSeasonExist = false;
            if (didShowExist) {
              queue(new SyncShow(traktId));
            }
          }
          collectedSeason = new CollectedSeason(seasonNumber, seasonId);
          collectedShow.seasons.put(seasonNumber, collectedSeason);
        }

        List<CollectionItem.Episode> episodes = season.getEpisodes();
        for (CollectionItem.Episode episode : episodes) {
          final int episodeNumber = episode.getNumber();
          CollectedEpisode syncEpisode = collectedSeason.episodes.get(episodeNumber);

          if (syncEpisode == null) {
            EpisodeDatabaseHelper.IdResult episodeResult =
                episodeHelper.getIdOrCreate(collectedShow.id, collectedSeason.id, episodeNumber);
            final long episodeId = episodeResult.id;
            if (episodeResult.didCreate) {
              if (didShowExist && didSeasonExist) {
                queue(new SyncSeason(traktId, seasonNumber));
              }
            }

            ContentProviderOperation.Builder builder =
                ContentProviderOperation.newUpdate(Episodes.withId(episodeId));
            ContentValues cv = new ContentValues();
            cv.put(EpisodeColumns.IN_COLLECTION, true);
            builder.withValues(cv);
            ops.add(builder.build());
          } else {
            episodeIds.remove(syncEpisode.id);
          }
        }
      }

      apply(ops);
    }

    ops.clear();
    for (long episodeId : episodeIds) {
      ContentProviderOperation.Builder builder =
          ContentProviderOperation.newUpdate(Episodes.withId(episodeId));
      ContentValues cv = new ContentValues();
      cv.put(EpisodeColumns.IN_COLLECTION, false);
      builder.withValues(cv);
      ops.add(builder.build());
    }
    apply(ops);
  }

  private void apply(ArrayList<ContentProviderOperation> ops) {
    try {
      getContentResolver().applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
      ops.clear();
    } catch (RemoteException e) {
      Timber.e(e, "SyncShowsWatchedTask failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncShowsWatchedTask failed");
      throw new JobFailedException(e);
    }
  }

  private static class CollectedShow {

    long traktId;

    long id;

    public CollectedShow(long traktId, long id) {
      this.traktId = traktId;
      this.id = id;
    }

    SparseArrayCompat<CollectedSeason> seasons = new SparseArrayCompat<>();
  }

  private static class CollectedSeason {

    int season;

    long id;

    public CollectedSeason(int season, long id) {
      this.season = season;
      this.id = id;
    }

    SparseArrayCompat<CollectedEpisode> episodes = new SparseArrayCompat<>();
  }

  private static class CollectedEpisode {

    long id;

    int number;

    public CollectedEpisode(long id, int number) {
      this.id = id;
      this.number = number;
    }
  }
}
