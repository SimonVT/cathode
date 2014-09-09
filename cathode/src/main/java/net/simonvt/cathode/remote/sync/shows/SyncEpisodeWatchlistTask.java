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
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class SyncEpisodeWatchlistTask extends TraktTask {

  @Inject transient SyncService syncService;

  @Override protected void doTask() {
    Cursor c = getContentResolver().query(Episodes.EPISODES_IN_WATCHLIST, new String[] {
        DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.ID,
    }, null, null, null);

    List<Long> episodeIds = new ArrayList<Long>();

    while (c.moveToNext()) {
      episodeIds.add(c.getLong(c.getColumnIndex(EpisodeColumns.ID)));
    }
    c.close();

    List<WatchlistItem> watchlist = syncService.getEpisodeWatchlist();

    for (WatchlistItem item : watchlist) {
      final Show show = item.getShow();
      final Episode episode = item.getEpisode();
      final long showTraktId = show.getIds().getTrakt();
      final int seasonNumber = episode.getSeason();
      final int episodeNumber = episode.getNumber();
      final long listedAt = item.getListedAt().getTimeInMillis();

      boolean didShowExist = true;
      long showId = ShowWrapper.getShowId(getContentResolver(), showTraktId);
      if (showId == -1L) {
        didShowExist = false;
        showId = ShowWrapper.createShow(getContentResolver(), showTraktId);
        queueTask(new SyncShowTask(showTraktId));
      }

      boolean didSeasonExist = true;
      long seasonId = SeasonWrapper.getSeasonId(getContentResolver(), showId, seasonNumber);
      if (seasonId == -1L) {
        didSeasonExist = false;
        seasonId = SeasonWrapper.createSeason(getContentResolver(), showId, seasonNumber);
        if (didShowExist) {
          queueTask(new SyncSeasonTask(showTraktId, seasonNumber));
        }
      }

      long episodeId =
          EpisodeWrapper.getEpisodeId(getContentResolver(), showId, seasonNumber, episodeNumber);
      if (episodeId == -1L) {
        EpisodeWrapper.createEpisode(getContentResolver(), showId, seasonId, episodeNumber);
        if (didShowExist && didSeasonExist) {
          queueTask(new SyncEpisodeTask(showTraktId, seasonNumber, episodeNumber));
        }
      }

      if (!episodeIds.remove(episodeId)) {
        EpisodeWrapper.setIsInWatchlist(getContentResolver(), episodeId, true, listedAt);
      }
    }

    for (Long episodeId : episodeIds) {
      EpisodeWrapper.setIsInWatchlist(getContentResolver(), episodeId, false, 0L);
    }

    postOnSuccess();
  }
}
