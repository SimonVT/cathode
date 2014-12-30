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
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.SeasonService;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class SyncShowTask extends TraktTask {

  @Inject transient ShowsService showsService;
  @Inject transient SeasonService seasonService;

  private long traktId;

  private boolean syncAdditionalInfo;

  public SyncShowTask(long traktId) {
    this(traktId, true);
  }

  public SyncShowTask(long traktId, boolean syncAdditionalInfo) {
    this.traktId = traktId;
    this.syncAdditionalInfo = syncAdditionalInfo;
  }

  @Override protected void doTask() {
    Show show = showsService.getSummary(traktId, Extended.FULL_IMAGES);
    final long showId = ShowWrapper.updateOrInsertShow(getContentResolver(), show);
    queueTask(new SyncShowCast(traktId));

    if (!syncAdditionalInfo) {
      postOnSuccess();
      return;
    }

    List<Season> seasons = seasonService.getSummary(traktId, Extended.FULL_IMAGES);

    List<TraktTask> pendingTasks = new ArrayList<TraktTask>();
    List<Long> episodeIds = new ArrayList<Long>();
    List<Long> seasonIds = new ArrayList<Long>();

    for (Season season : seasons) {
      // TODO: Once unique tasks are supported, don't do this
      final long seasonId =
          SeasonWrapper.updateOrInsertSeason(getContentResolver(), season, showId);
      seasonIds.add(seasonId);
      List<Episode> episodes = seasonService.getSeason(traktId, season.getNumber());
      for (Episode episode : episodes) {
        long episodeId =
            EpisodeWrapper.getEpisodeId(getContentResolver(), showId, episode.getSeason(),
                episode.getNumber());
        if (episodeId == -1L) {
          episodeId = EpisodeWrapper.createEpisode(getContentResolver(), showId, seasonId,
              episode.getNumber());
        }
        episodeIds.add(episodeId);
        pendingTasks.add(new SyncEpisodeTask(traktId, season.getNumber(), episode.getNumber()));
      }
    }

    Cursor allEpisodes = getContentResolver().query(Episodes.fromShow(showId), new String[] {
        EpisodeColumns.ID,
    }, null, null, null);
    while (allEpisodes.moveToNext()) {
      final long episodeId = allEpisodes.getLong(allEpisodes.getColumnIndex(EpisodeColumns.ID));
      if (!episodeIds.contains(episodeId)) {
        getContentResolver().delete(Episodes.withId(episodeId), null, null);
      }
    }
    allEpisodes.close();

    Cursor allSeasons = getContentResolver().query(Seasons.fromShow(showId), new String[] {
        SeasonColumns.ID,
    }, null, null, null);
    while (allSeasons.moveToNext()) {
      final long seasonId = allSeasons.getLong(allSeasons.getColumnIndex(SeasonColumns.ID));
      if (!seasonIds.contains(seasonId)) {
        getContentResolver().delete(Episodes.fromSeason(seasonId), null, null);
        getContentResolver().delete(Seasons.withId(seasonId), null, null);
      }
    }
    allSeasons.close();

    for (TraktTask task : pendingTasks) {
      queueTask(task);
    }

    queueTask(new SyncShowCollectedStatus(traktId));
    queueTask(new SyncShowWatchedStatus(traktId));

    postOnSuccess();
  }
}
