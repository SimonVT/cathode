/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.SeasonService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import retrofit2.Call;

public class SyncSeasons extends CallJob<List<Season>> {

  @Inject transient SeasonService seasonService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;

  long traktId;

  public SyncSeasons(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncSeasons" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return JobPriority.SEASONS;
  }

  @Override public Call<List<Season>> getCall() {
    return seasonService.getSummary(traktId, Extended.FULL);
  }

  @Override public boolean handleResponse(List<Season> seasons) {
    final long showId = showHelper.getId(traktId);
    if (showId == -1L) {
      queue(new SyncShow(traktId));
      return true;
    }

    List<Long> seasonIds = new ArrayList<>();
    Cursor currentSeasons = getContentResolver().query(Seasons.fromShow(showId), new String[] {
        SeasonColumns.ID,
    }, null, null, null);
    while (currentSeasons.moveToNext()) {
      final long id = Cursors.getLong(currentSeasons, SeasonColumns.ID);
      seasonIds.add(id);
    }
    currentSeasons.close();

    for (Season season : seasons) {
      SeasonDatabaseHelper.IdResult result = seasonHelper.getIdOrCreate(showId, season.getNumber());
      seasonHelper.updateSeason(showId, season);
      seasonIds.remove(result.id);
      queue(new SyncSeason(traktId, season.getNumber()));
    }

    for (Long seasonId : seasonIds) {
      getContentResolver().delete(Seasons.withId(seasonId), null, null);
    }

    return true;
  }
}
