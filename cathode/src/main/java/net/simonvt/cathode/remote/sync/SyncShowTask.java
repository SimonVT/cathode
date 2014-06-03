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

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.ResponseParser;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncShowTask extends TraktTask {

  @Inject transient ShowService showService;

  private int tvdbId;

  public SyncShowTask(int tvdbId) {
    this.tvdbId = tvdbId;
  }

  @Override protected void doTask() {
    try {
      TvShow show = showService.summary(tvdbId, DetailLevel.EXTENDED);
      final long showId = ShowWrapper.updateOrInsertShow(getContentResolver(), show);

      List<Long> episodeIds = new ArrayList<Long>();
      List<Long> seasonIds = new ArrayList<Long>();

      List<Season> seasons = show.getSeasons();
      for (Season season : seasons) {
        final long seasonId =
            SeasonWrapper.updateOrInsertSeason(getContentResolver(), season, showId);
        seasonIds.add(seasonId);

        List<Episode> episodes = season.getEpisodes().getEpisodes();
        for (Episode episode : episodes) {
          final long episodeId =
              EpisodeWrapper.updateOrInsertEpisode(getContentResolver(), episode, showId, seasonId);
          episodeIds.add(episodeId);
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

      postOnSuccess();
    } catch (RetrofitError e) {
      retrofit.client.Response r = e.getResponse();
      if (r != null) {
        ResponseParser parser = new ResponseParser();
        CathodeApp.inject(getContext(), parser);
        Response response = parser.tryParse(e);
        if (response != null && "show not found".equals(response.getError())) {
          postOnSuccess();
          return;
        }
      }
      logError(e);
      postOnFailure();
    }
  }
}
