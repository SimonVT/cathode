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
import net.simonvt.cathode.api.entity.SyncResponse;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit.Call;

public class WatchedSeason extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  @Inject transient SeasonDatabaseHelper seasonHelper;

  private long traktId;

  private int season;

  private boolean watched;

  private String watchedAt;

  public WatchedSeason(long traktId, int season, boolean watched, String watchedAt) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.season = season;
    this.watched = watched;
    this.watchedAt = watchedAt;
  }

  @Override public String key() {
    return "WatchedSeason"
        + "&traktId="
        + traktId
        + "&season="
        + season
        + "&watched="
        + watched
        + "&watchedAt="
        + watchedAt;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public Call<SyncResponse> getCall() {
    if (watched) {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season).watchedAt(watchedAt);
      return syncService.watched(items);
    } else {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season);
      return syncService.unwatched(items);
    }
  }

  @Override public void handleResponse(SyncResponse response) {
    long watchedAt = 0L;
    if (watched) {
      watchedAt = TimeUtils.getMillis(this.watchedAt);
    }

    seasonHelper.setWatched(traktId, season, watched, watchedAt);
  }
}
