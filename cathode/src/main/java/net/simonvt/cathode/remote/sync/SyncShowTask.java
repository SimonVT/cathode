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
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncShowTask extends TraktTask {

  private static final String TAG = "SyncShowTask";

  @Inject transient ShowService showService;

  private final int tvdbId;

  public SyncShowTask(int tvdbId) {
    this.tvdbId = tvdbId;
  }

  @Override protected void doTask() {
    try {
      TvShow show = showService.summary(tvdbId, DetailLevel.EXTENDED);
      final long showId = ShowWrapper.updateOrInsertShow(service.getContentResolver(), show);

      List<Season> seasons = show.getSeasons();

      for (Season season : seasons) {
        final long seasonId =
            SeasonWrapper.updateOrInsertSeason(service.getContentResolver(), season, showId);
        List<Episode> episodes = season.getEpisodes().getEpisodes();
        for (Episode episode : episodes) {
          EpisodeWrapper.updateOrInsertEpisode(service.getContentResolver(), episode, showId,
              seasonId);
        }
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
