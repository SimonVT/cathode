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

import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.EpisodeService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SyncEpisode extends Job {

  @Inject transient EpisodeService episodeService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

  private long traktId;

  private int season;

  private int episode;

  public SyncEpisode(long traktId, int season, int episode) {
    this.traktId = traktId;
    this.season = season;
    this.episode = episode;
  }

  @Override public String key() {
    return "SyncEpisode" + "&traktId=" + traktId + "&season=" + season + "&episode=" + episode;
  }

  @Override public int getPriority() {
    return PRIORITY_SEASONS;
  }

  @Override public void perform() {
    try {
      Episode summary = episodeService.getSummary(traktId, season, episode, Extended.FULL_IMAGES);

      final long showId = showHelper.getId(traktId);
      final long seasonId = seasonHelper.getId(showId, season);

      if (showId == -1L || seasonId == -1L) {
        queue(new SyncShow(traktId));
        return;
      }

      EpisodeDatabaseHelper.IdResult episodeResult =
          episodeHelper.getIdOrCreate(showId, seasonId, summary.getNumber());
      episodeHelper.updateEpisode(showId, summary);
    } catch (RetrofitError e) {
      Response response = e.getResponse();
      if (response != null && response.getStatus() == 404) {
        // Episode no longer exists
      } else {
        throw new JobFailedException(e);
      }
    }
  }
}
