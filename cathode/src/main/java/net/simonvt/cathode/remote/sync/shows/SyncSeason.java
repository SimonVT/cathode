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
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.SeasonService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import timber.log.Timber;

public class SyncSeason extends Job {

  @Inject transient SeasonService seasonService;

  private long traktId;

  private int season;

  public SyncSeason(long traktId, int season) {
    this.traktId = traktId;
    this.season = season;
  }

  @Override public String key() {
    return "SyncSeason" + "&traktId=" + traktId + "&season=" + season;
  }

  @Override public void perform() {
    Timber.d("Syncing season %d of show %d", season, traktId);

    List<Episode> episodes = seasonService.getSeason(traktId, season, Extended.FULL_IMAGES);

    long showId = ShowWrapper.getShowId(getContentResolver(), traktId);
    if (showId == -1L) {
      showId = ShowWrapper.createShow(getContentResolver(), traktId);
      queue(new SyncShow(traktId));
    }
    long seasonId = SeasonWrapper.getSeasonId(getContentResolver(), showId, season);
    if (seasonId == -1L) {
      seasonId = SeasonWrapper.createSeason(getContentResolver(), showId, season);
      queue(new SyncSeasons(traktId));
    }

    Cursor c = getContentResolver().query(Episodes.fromSeason(seasonId), new String[] {
        EpisodeColumns.ID
    }, null, null, null);
    List<Long> episodeIds = new ArrayList<Long>();
    while (c.moveToNext()) {
      final long episodeId = c.getLong(c.getColumnIndex(EpisodeColumns.ID));
      episodeIds.add(episodeId);
    }
    c.close();

    for (Episode episode : episodes) {
      final long episodeId =
          EpisodeWrapper.updateOrInsertEpisode(getContentResolver(), episode, showId, seasonId);
      episodeIds.remove(episodeId);
    }

    for (Long episodeId : episodeIds) {
      getContentResolver().delete(Episodes.withId(episodeId), null, null);
    }
  }
}
