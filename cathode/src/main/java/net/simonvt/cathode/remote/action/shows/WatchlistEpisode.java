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
import net.simonvt.cathode.api.entity.SyncResponse;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit.Call;

public class WatchlistEpisode extends CallJob<SyncResponse> {

  @Inject transient SyncService syncService;

  @Inject transient EpisodeDatabaseHelper episodeHelper;

  private long traktId;

  private int season;

  private int episode;

  private boolean inWatchlist;

  private String listedAt;

  public WatchlistEpisode(long traktId, int season, int episode, boolean inWatchlist,
      String listedAt) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.season = season;
    this.episode = episode;
    this.inWatchlist = inWatchlist;
    this.listedAt = listedAt;
  }

  @Override public String key() {
    return "WatchlistEpisode"
        + "&traktId="
        + traktId
        + "&season="
        + season
        + "&episode="
        + episode
        + "&inWatchlist="
        + inWatchlist
        + "&listedAt="
        + listedAt;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public Call<SyncResponse> getCall() {
    if (inWatchlist) {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season).episode(episode).listedAt(listedAt);
      return syncService.watchlist(items);
    } else {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season).episode(episode);
      return syncService.unwatchlist(items);
    }
  }

  @Override public void handleResponse(SyncResponse response) {
    episodeHelper.setIsInWatchlist(traktId, season, episode, inWatchlist,
        TimeUtils.getMillis(listedAt));
  }
}
