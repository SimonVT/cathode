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
package net.simonvt.cathode.remote.action.movies;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.entity.SyncResponse;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import retrofit2.Call;

public class CollectMovie extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  private long traktId;
  private boolean collected;
  private String collectedAt;

  public CollectMovie(long traktId, boolean collected, String collectedAt) {

    this.traktId = traktId;
    this.collected = collected;
    this.collectedAt = collectedAt;
  }

  @Override public String key() {
    return "CollectMovie"
        + "&traktId="
        + traktId
        + "&collected="
        + collected
        + "&collectedAt="
        + collectedAt;
  }

  @Override public Call<SyncResponse> getCall() {
    if (collected) {
      return syncService.collect(
          new SyncItems.Builder().movie(traktId, null, collectedAt, null).build());
    } else {
      return syncService.uncollect(
          new SyncItems.Builder().movie(traktId, null, null, null).build());
    }
  }

  @Override public boolean handleResponse(SyncResponse response) {
    queue(new SyncUserActivity());
    return true;
  }
}
