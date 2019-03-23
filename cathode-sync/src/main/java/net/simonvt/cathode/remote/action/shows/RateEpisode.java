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
import net.simonvt.cathode.api.body.RateItems;
import net.simonvt.cathode.api.entity.SyncResponse;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import retrofit2.Call;

public class RateEpisode extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  private long traktId;
  private int season;
  private int episode;
  private int rating;
  private String ratedAt;

  public RateEpisode(long traktId, int season, int episode, int rating, String ratedAt) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.season = season;
    this.episode = episode;
    this.rating = rating;
    this.ratedAt = ratedAt;
  }

  @Override public String key() {
    return "RateEpisode"
        + "&traktId="
        + traktId
        + "&season="
        + season
        + "&episode="
        + episode
        + "&rating="
        + rating
        + "&ratedAt="
        + ratedAt;
  }

  @Override public int getPriority() {
    return JobPriority.ACTIONS;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public Call<SyncResponse> getCall() {
    RateItems items =
        new RateItems.Builder().episode(traktId, season, episode, rating, ratedAt).build();
    return syncService.rate(items);
  }

  @Override public boolean handleResponse(SyncResponse response) {
    queue(new SyncUserActivity());
    return true;
  }
}
