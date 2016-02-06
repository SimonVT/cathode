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

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.entity.WatchlistItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class SyncEpisodeWatchlist extends CallJob<List<WatchlistItem>> {

  @Inject transient SyncService syncService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

  public SyncEpisodeWatchlist() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncEpisodeWatchlist";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public Call<List<WatchlistItem>> getCall() {
    return syncService.getEpisodeWatchlist();
  }

  @Override public void handleResponse(List<WatchlistItem> watchlist) {
    Cursor c = getContentResolver().query(Episodes.EPISODES_IN_WATCHLIST, new String[] {
        DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.ID,
    }, null, null, null);

    List<Long> episodeIds = new ArrayList<Long>();

    while (c.moveToNext()) {
      episodeIds.add(c.getLong(c.getColumnIndex(EpisodeColumns.ID)));
    }
    c.close();

    for (WatchlistItem item : watchlist) {
      final Show show = item.getShow();
      final Episode episode = item.getEpisode();
      final long showTraktId = show.getIds().getTrakt();
      final int seasonNumber = episode.getSeason();
      final int episodeNumber = episode.getNumber();
      final long listedAt = item.getListedAt().getTimeInMillis();

      ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(showTraktId);
      final long showId = showResult.showId;
      final boolean didShowExist = !showResult.didCreate;
      if (showResult.didCreate) {
        queue(new SyncShow(showTraktId));
      }

      SeasonDatabaseHelper.IdResult seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber);
      final long seasonId = seasonResult.id;
      final boolean didSeasonExist = !seasonResult.didCreate;
      if (seasonResult.didCreate) {
        if (didShowExist) {
          queue(new SyncShow(showTraktId, true));
        }
      }

      EpisodeDatabaseHelper.IdResult episodeResult =
          episodeHelper.getIdOrCreate(showId, seasonId, episodeNumber);
      final long episodeId = episodeResult.id;
      if (episodeResult.didCreate) {
        if (didShowExist && didSeasonExist) {
          queue(new SyncSeason(showTraktId, seasonNumber));
        }
      }

      if (!episodeIds.remove(episodeId)) {
        episodeHelper.setIsInWatchlist(episodeId, true, listedAt);
      }
    }

    for (Long episodeId : episodeIds) {
      episodeHelper.setIsInWatchlist(episodeId, false, 0L);
    }
  }
}
