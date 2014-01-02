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
package net.simonvt.cathode.remote.sync;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncShowSeasonsTask extends TraktTask {

  @Inject transient ShowService showService;

  private int tvdbId;

  public SyncShowSeasonsTask(int tvdbId) {
    this.tvdbId = tvdbId;
  }

  @Override protected void doTask() {
    final long showId = ShowWrapper.getShowId(service.getContentResolver(), tvdbId);

    List<Season> seasons = showService.seasons(tvdbId);

    for (Season season : seasons) {
      Timber.d("Scheduling sync for season %d of show %d", season.getSeason(), tvdbId);
      SeasonWrapper.updateOrInsertSeason(service.getContentResolver(), season, showId);
      queueTask(new SyncSeasonTask(tvdbId, season.getSeason()));
    }

    postOnSuccess();
  }
}
