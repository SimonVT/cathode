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
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class SyncSeasonsTask extends TraktTask {

  @Inject transient SeasonService seasonService;

  long traktId;

  public SyncSeasonsTask(long traktId) {
    this.traktId = traktId;
  }

  @Override protected void doTask() {
    final long showId = ShowWrapper.getShowId(getContentResolver(), traktId);
    if (showId == -1L) {
      queueTask(new SyncShowTask(traktId));
      return;
    }

    List<Season> seasons = seasonService.getSummary(traktId, Extended.IMAGES);

    List<Long> seasonIds = new ArrayList<Long>();
    Cursor currentSeasons = getContentResolver().query(Seasons.fromShow(showId), new String[] {
        SeasonColumns.ID,
    }, null, null, null);
    while (currentSeasons.moveToNext()) {
      final long id = currentSeasons.getLong(currentSeasons.getColumnIndex(SeasonColumns.ID));
      seasonIds.add(id);
    }
    currentSeasons.close();

    for (Season season : seasons) {
      final long seasonId =
          SeasonWrapper.updateOrInsertSeason(getContentResolver(), season, showId);
      seasonIds.remove(seasonId);
      queueTask(new SyncSeasonTask(traktId, season.getNumber()));
    }

    for (Long seasonId : seasonIds) {
      getContentResolver().delete(Seasons.withId(seasonId), null, null);
    }
  }
}
