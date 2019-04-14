/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import retrofit2.Call;

public class AddEpisodeToHistory extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  private long traktId;
  private int season;
  private int episode;
  private String watchedAt;

  public AddEpisodeToHistory(long traktId, int season, int episode, String watchedAt) {

    if (traktId == 0) throw new IllegalArgumentException("tvdb is 0");
    this.traktId = traktId;
    this.season = season;
    this.episode = episode;
    this.watchedAt = watchedAt;
  }

  @Override public String key() {
    return "AddEpisodeToHistory"
        + "&traktId="
        + traktId
        + "&season="
        + season
        + "&episode="
        + episode
        + "&watchedAt="
        + watchedAt;
  }

  @Override public Call<SyncResponse> getCall() {
    SyncItems items =
        new SyncItems.Builder().episode(traktId, season, episode, watchedAt, null, null).build();
    return syncService.watched(items);
  }

  @Override public boolean handleResponse(SyncResponse response) {
    queue(new SyncUserActivity());
    return true;
  }
}
