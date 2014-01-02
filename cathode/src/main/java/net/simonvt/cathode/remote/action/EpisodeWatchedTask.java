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
package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.ShowEpisodeBody;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class EpisodeWatchedTask extends TraktTask {

  @Inject transient ShowService showService;

  private int tvdbId;

  private int season;

  private int episode;

  private boolean watched;

  public EpisodeWatchedTask(int tvdbId, int season, int episode, boolean watched) {
    if (tvdbId == 0) throw new IllegalArgumentException("tvdb is 0");
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
    this.watched = watched;
  }

  @Override protected void doTask() {
    try {
      if (watched) {
        Response response = showService.episodeSeen(new ShowEpisodeBody(tvdbId, season, episode));
      } else {
        Response response = showService.episodeUnseen(new ShowEpisodeBody(tvdbId, season, episode));
      }

      EpisodeWrapper.setWatched(service.getContentResolver(), tvdbId, season, episode, watched);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
