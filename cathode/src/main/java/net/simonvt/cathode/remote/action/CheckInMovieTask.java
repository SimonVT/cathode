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
import net.simonvt.cathode.api.service.MovieService;
import net.simonvt.cathode.remote.TraktTask;

public class CheckInMovieTask extends TraktTask {

  @Inject transient MovieService movieService;

  long tmdbId;

  String message;

  boolean facebook;

  boolean twitter;

  boolean tumblr;

  boolean path;

  boolean prowl;

  public CheckInMovieTask(long tmdbId, String message, boolean facebook, boolean twitter,
      boolean tumblr, boolean path, boolean prowl) {
    this.tmdbId = tmdbId;
    this.message = message;
    this.facebook = facebook;
    this.twitter = twitter;
    this.tumblr = tumblr;
    this.path = path;
    this.prowl = prowl;
  }

  @Override protected void doTask() {
    CheckinBody body = CheckinBody.tmdbId(tmdbId)
        .message(message)
        .facebook(facebook)
        .twitter(twitter)
        .tumblr(tumblr)
        .path(path)
        .prowl(prowl);

    movieService.checkin(body);
    postOnSuccess();
  }
}
