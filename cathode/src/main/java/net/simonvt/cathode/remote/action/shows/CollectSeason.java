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
import retrofit2.Call;

public class CollectSeason extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  @Inject transient SeasonDatabaseHelper seasonHelper;

  private long traktId;

  private int season;

  private boolean inCollection;

  private String collectedAt;

  public CollectSeason(long traktId, int season, boolean inCollection, String collectedAt) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.season = season;
    this.inCollection = inCollection;
    this.collectedAt = collectedAt;
  }

  @Override public String key() {
    return "CollectSeason"
        + "&traktId="
        + traktId
        + "&season="
        + season
        + "&inCollection="
        + inCollection
        + "&collectedAt="
        + collectedAt;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public Call<SyncResponse> getCall() {
    if (inCollection) {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season).collectedAt(collectedAt);
      return syncService.collect(items);
    } else {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season);
      return syncService.uncollect(items);
    }
  }

  @Override public void handleResponse(SyncResponse response) {
    seasonHelper.setIsInCollection(traktId, season, inCollection, TimeUtils.getMillis(collectedAt));
  }
}
