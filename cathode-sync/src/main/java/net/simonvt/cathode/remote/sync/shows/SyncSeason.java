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

import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.SeasonService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;

public class SyncSeason extends CallJob<List<Episode>> {

  @Inject transient SeasonService seasonService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

  private long traktId;

  private int season;

  public SyncSeason(long traktId, int season) {
    this.traktId = traktId;
    this.season = season;
  }

  @Override public String key() {
    return "SyncSeason" + "&traktId=" + traktId + "&season=" + season;
  }

  @Override public int getPriority() {
    return JobPriority.SEASONS;
  }

  @Override public Call<List<Episode>> getCall() {
    return seasonService.getSeason(traktId, season, Extended.FULL);
  }

  @Override public boolean handleResponse(List<Episode> episodes) {
    ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
    final long showId = showResult.showId;
    final boolean didShowExist = !showResult.didCreate;
    if (showResult.didCreate) {
      queue(new SyncShow(traktId));
    }

    SeasonDatabaseHelper.IdResult seasonResult = seasonHelper.getIdOrCreate(showId, season);
    final long seasonId = seasonResult.id;
    if (seasonResult.didCreate) {
      if (didShowExist) {
        queue(new SyncShow(traktId));
      }
    }

    Cursor c = getContentResolver().query(Episodes.fromSeason(seasonId), new String[] {
        EpisodeColumns.ID
    }, null, null, null);
    List<Long> episodeIds = new ArrayList<>();
    while (c.moveToNext()) {
      final long episodeId = Cursors.getLong(c, EpisodeColumns.ID);
      episodeIds.add(episodeId);
    }
    c.close();

    for (Episode episode : episodes) {
      EpisodeDatabaseHelper.IdResult episodeResult =
          episodeHelper.getIdOrCreate(showId, seasonId, episode.getNumber());
      final long episodeId = episodeResult.id;
      episodeHelper.updateEpisode(episodeId, episode);
      episodeIds.remove(episodeId);
    }

    for (Long episodeId : episodeIds) {
      getContentResolver().delete(Episodes.withId(episodeId), null, null);
    }

    ContentValues values = new ContentValues();
    values.put(SeasonColumns.NEEDS_SYNC, false);
    getContentResolver().update(Seasons.withId(seasonId), values, null, null);
    return true;
  }
}
