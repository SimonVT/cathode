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
package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.CheckinBody;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.remote.TraktTask;

public class CheckInEpisodeTask extends TraktTask {

  @Inject transient ShowService showService;

  private int tvdbId;

  private int season;

  private int episode;

  String message;

  boolean facebook;

  boolean twitter;

  boolean tumblr;

  boolean path;

  boolean prowl;

  public CheckInEpisodeTask(int tvdbId, int season, int episode, String message, boolean facebook,
      boolean twitter, boolean tumblr, boolean path, boolean prowl) {
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
    this.message = message;
    this.facebook = facebook;
    this.twitter = twitter;
    this.tumblr = tumblr;
    this.path = path;
    this.prowl = prowl;
  }

  @Override protected void doTask() {
    CheckinBody body = CheckinBody.tvdbId(tvdbId)
        .season(season)
        .episode(episode)
        .message(message)
        .facebook(facebook)
        .twitter(twitter)
        .tumblr(tumblr)
        .path(path)
        .prowl(prowl);

    showService.checkin(body);
    postOnSuccess();
  }
}
