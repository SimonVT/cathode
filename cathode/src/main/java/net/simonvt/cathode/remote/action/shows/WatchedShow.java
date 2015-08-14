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
package net.simonvt.cathode.remote.action.shows;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;

public class WatchedShow extends Job {

  @Inject transient SyncService syncService;

  private long traktId;

  private boolean watched;

  private long timestamp;

  public WatchedShow(long traktId, boolean watched) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.watched = watched;
    this.timestamp = System.currentTimeMillis();
  }

  @Override public String key() {
    return "WatchedShow"
        + "&traktId="
        + traktId
        + "&watched="
        + watched
        + "&timestamp="
        + timestamp;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public void perform() {
    SyncItems items = new SyncItems();
    items.show(traktId);
    if (watched) {
      syncService.watched(items);
    } else {
      syncService.unwatched(items);
    }
  }
}
