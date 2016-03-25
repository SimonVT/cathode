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
import net.simonvt.cathode.util.Cursors;
import retrofit2.Call;
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
      Cursor c = getContentResolver().query(Episodes.EPISODES, new String[] {
          EpisodeColumns.ID, EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON,
          EpisodeColumns.SEASON_ID, EpisodeColumns.EPISODE,
      }, EpisodeColumns.WATCHED, null, null);

      LongSparseArray<WatchedShow> showsMap = new LongSparseArray<>();
      LongSparseArray<Long> showIdToTraktMap = new LongSparseArray<>();
      List<Long> episodeIds = new ArrayList<>(c.getCount());

      while (c.moveToNext()) {
        final long id = Cursors.getLong(c, EpisodeColumns.ID);
        final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
        final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
        final long seasonId = Cursors.getLong(c, EpisodeColumns.SEASON_ID);

        WatchedShow watchedShow;
        Long showTraktId = showIdToTraktMap.get(showId);
        if (showTraktId == null) {
          showTraktId = showHelper.getTraktId(showId);

          showIdToTraktMap.put(showId, showTraktId);

          watchedShow = new WatchedShow(showTraktId, showId);
          showsMap.put(showTraktId, watchedShow);
        } else {
          watchedShow = showsMap.get(showTraktId);
        }

        WatchedSeason syncSeason = watchedShow.seasons.get(season);
        if (syncSeason == null) {
          syncSeason = new WatchedSeason(season, seasonId);
          watchedShow.seasons.put(season, syncSeason);
        }

        final int number = Cursors.getInt(c, EpisodeColumns.EPISODE);

        WatchedEpisode syncEpisode = syncSeason.episodes.get(number);
        if (syncEpisode == null) {
          syncEpisode = new WatchedEpisode(id, number);
          syncSeason.episodes.put(number, syncEpisode);
        }

        episodeIds.add(id);
      }
      c.close();

      ArrayList<ContentProviderOperation> ops = new ArrayList<>();

      Timber.d("Processing items");
      for (WatchedItem item : watched) {
        Show show = item.getShow();
        final long traktId = show.getIds().getTrakt();

        WatchedShow watchedShow = showsMap.get(traktId);

        boolean didShowExist = true;
        if (watchedShow == null) {
          ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
          final long showId = showResult.showId;
          if (showResult.didCreate) {
            didShowExist = false;
            queue(new SyncShow(traktId));
          }
          watchedShow = new WatchedShow(traktId, showId);
          showsMap.put(traktId, watchedShow);
        }

        IsoTime lastWatched = item.getLastWatchedAt();
        final long lastWatchedMillis = lastWatched.getTimeInMillis();

        ops.add(ContentProviderOperation.newUpdate(ProviderSchematic.Shows.withId(watchedShow.id))
            .withValue(DatabaseContract.ShowColumns.LAST_WATCHED_AT, lastWatchedMillis)
            .build());

        List<WatchedItem.Season> seasons = item.getSeasons();
        for (WatchedItem.Season season : seasons) {
          final int seasonNumber = season.getNumber();
          WatchedSeason watchedSeason = watchedShow.seasons.get(seasonNumber);
          boolean didSeasonExist = true;
          if (watchedSeason == null) {
            SeasonDatabaseHelper.IdResult seasonResult =
                seasonHelper.getIdOrCreate(watchedShow.id, seasonNumber);
            final long seasonId = seasonResult.id;
            if (seasonResult.didCreate) {
              didSeasonExist = false;
              if (didShowExist) {
                queue(new SyncShow(traktId, true));
              }
            }
            watchedSeason = new WatchedSeason(seasonNumber, seasonId);
            watchedShow.seasons.put(seasonNumber, watchedSeason);
          }

          List<WatchedItem.Episode> episodes = season.getEpisodes();
          for (WatchedItem.Episode episode : episodes) {
            final int episodeNumber = episode.getNumber();
            WatchedEpisode syncEpisode = watchedSeason.episodes.get(episodeNumber);

            if (syncEpisode == null) {
              EpisodeDatabaseHelper.IdResult episodeResult =
                  episodeHelper.getIdOrCreate(watchedShow.id, watchedSeason.id, episodeNumber);
              final long episodeId = episodeResult.id;
              if (episodeResult.didCreate) {
                if (didShowExist && didSeasonExist) {
                  queue(new SyncSeason(traktId, seasonNumber));
                }
              }

              ContentProviderOperation.Builder builder =
                  ContentProviderOperation.newUpdate(Episodes.withId(episodeId));
              ContentValues cv = new ContentValues();
              cv.put(EpisodeColumns.WATCHED, true);
              builder.withValues(cv);
              ops.add(builder.build());
            } else {
              episodeIds.remove(syncEpisode.id);
            }
          }
        }
      }
      Timber.d("Done processing items");

      for (long episodeId : episodeIds) {
        ContentProviderOperation.Builder builder =
            ContentProviderOperation.newUpdate(Episodes.withId(episodeId));
        ContentValues cv = new ContentValues();
        cv.put(EpisodeColumns.WATCHED, false);
        builder.withValues(cv);
        ops.add(builder.build());
      }

      getContentResolver().applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
      Timber.d("Done applying items");
    } catch (RemoteException e) {
      Timber.e(e, "SyncShowsWatchedTask failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncShowsWatchedTask failed");
      throw new JobFailedException(e);
    }
  }

  private static class WatchedShow {

    long traktId;

    long id;

    public WatchedShow(long traktId, long id) {
      this.traktId = traktId;
      this.id = id;
    }

    SparseArrayCompat<WatchedSeason> seasons = new SparseArrayCompat<>();
  }

  private static class WatchedSeason {

    int season;

    long id;

    public WatchedSeason(int season, long id) {
      this.season = season;
      this.id = id;
    }

    SparseArrayCompat<WatchedEpisode> episodes = new SparseArrayCompat<>();
  }

  private static class WatchedEpisode {

    long id;

    int number;

    public WatchedEpisode(long id, int number) {
      this.id = id;
      this.number = number;
    }
  }
}
