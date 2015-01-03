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
package net.simonvt.cathode.remote.action.movies;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.CheckinItem;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;

public class CheckInMovie extends Job {

  @Inject transient CheckinService checkinService;

  long traktId;

  String message;

  boolean facebook;

  boolean twitter;

  boolean tumblr;

  public CheckInMovie(long traktId, String message, boolean facebook, boolean twitter,
      boolean tumblr) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.message = message;
    this.facebook = facebook;
    this.twitter = twitter;
    this.tumblr = tumblr;
  }

  @Override public String key() {
    return "CheckInMovie" + "&traktId=" + traktId + "&message=" + message;
  }

  @Override public int getPriority() {
    return PRIORITY_5;
  }

  @Override public boolean requiresWakelock() {
    return true;
  }

  @Override public void perform() {
    CheckinItem item = new CheckinItem() //
        .episode(traktId) //
        .message(message) //
        .facebook(facebook) //
        .twitter(twitter) //
        .tumblr(tumblr);
    checkinService.checkin(item);
  }
}
