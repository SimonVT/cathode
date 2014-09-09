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
package net.simonvt.cathode.remote.action.shows;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class SeasonWatchedTask extends TraktTask {

  @Inject transient SyncService syncService;

  private long traktId;

  private int season;

  private boolean watched;

  private String watchedAt;

  public SeasonWatchedTask(long traktId, int season, boolean watched, String watchedAt) {
    this.traktId = traktId;
    this.season = season;
    this.watched = watched;
    this.watchedAt = watchedAt;
  }

  @Override protected void doTask() {
    if (watched) {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season).watchedAt(watchedAt);
      syncService.watched(items);
    } else {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season);
      syncService.unwatched(items);
    }

    long watchedAt = 0L;
    if (watched) {
      watchedAt = TimeUtils.getMillis(this.watchedAt);
    }

    SeasonWrapper.setWatched(getContentResolver(), traktId, season, watched, watchedAt);
    postOnSuccess();
  }
}
