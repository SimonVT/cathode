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
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;

public class WatchlistEpisode extends Job {

  @Inject transient SyncService syncService;

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

  @Override public void perform() {
    if (inWatchlist) {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season).episode(episode).listedAt(listedAt);
      syncService.watchlist(items);
    } else {
      SyncItems items = new SyncItems();
      items.show(traktId).season(season).episode(episode);
      syncService.unwatchlist(items);
    }

    EpisodeWrapper.setIsInWatchlist(getContentResolver(), traktId, season, episode, inWatchlist,
        TimeUtils.getMillis(listedAt));
  }
}
