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
import net.simonvt.cathode.api.body.RateItems;
import net.simonvt.cathode.api.entity.SyncResponse;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class RateMovie extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  private long traktId;

  private int rating;

  String ratedAt;

  public RateMovie(long traktId, int rating, String ratedAt) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.rating = rating;
    this.ratedAt = ratedAt;
  }

  @Override public String key() {
    return "RateMovie" + "&traktId=" + traktId + "&rating=" + rating + "&ratedAt=" + ratedAt;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public Call<SyncResponse> getCall() {
    RateItems items = new RateItems();
    items.movie(traktId).rating(rating).ratedAt(ratedAt);
    return syncService.rate(items);
  }

  @Override public void handleResponse(SyncResponse response) {
  }
}
