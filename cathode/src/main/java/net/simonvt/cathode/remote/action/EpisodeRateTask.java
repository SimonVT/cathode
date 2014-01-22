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
import net.simonvt.cathode.api.body.RateBody;
import net.simonvt.cathode.api.service.RateService;
import net.simonvt.cathode.remote.TraktTask;

public class EpisodeRateTask extends TraktTask {

  @Inject transient RateService rateService;

  private int tvdbId;

  private int season;

  private int episode;

  private int rating;

  public EpisodeRateTask(int tvdbId, int season, int episode, int rating) {
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
    this.rating = rating;
  }

  @Override protected void doTask() {
    rateService.episode(new RateBody().episode(tvdbId, season, episode, rating));
    postOnSuccess();
  }
}
