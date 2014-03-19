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
package net.simonvt.cathode.remote.sync;

import javax.inject.Inject;
import net.simonvt.cathode.api.entity.LastActivity;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.TraktTimestamps;

public class SyncUserActivityTask extends TraktTask {

  @Inject transient UserService userService;

  @Override protected void doTask() {
    LastActivity lastActivity = userService.lastActivity();

    long episodeLastWatched = lastActivity.getEpisode().getWatched();
    long episodeLastCollected = lastActivity.getEpisode().getCollection();
    long episodeLastWatchlist = lastActivity.getEpisode().getWatchlist();

    long showLastWatchlist = lastActivity.getShow().getWatchlist();

    long movieLastWatched = lastActivity.getMovie().getWatched();
    long movieLastCollected = lastActivity.getMovie().getCollection();
    long movieLastWatchlist = lastActivity.getMovie().getWatchlist();

    if (TraktTimestamps.episodeWatchedNeedsUpdate(getContext(), episodeLastWatched)) {
      queueTask(new SyncShowsWatchedTask());
    }

    if (TraktTimestamps.episodeCollectedNeedsUpdate(getContext(), episodeLastCollected)) {
      queueTask(new SyncShowsCollectionTask());
    }

    if (TraktTimestamps.episodeWatchlistNeedsUpdate(getContext(), episodeLastWatchlist)) {
      queueTask(new SyncEpisodeWatchlistTask());
    }

    if (TraktTimestamps.showWatchlistNeedsUpdate(getContext(), showLastWatchlist)) {
      queueTask(new SyncShowsWatchlistTask());
    }

    if (TraktTimestamps.movieWatchedNeedsUpdate(getContext(), movieLastWatched)) {
      queueTask(new SyncMoviesWatchedTask());
    }

    if (TraktTimestamps.movieCollectedNeedsUpdate(getContext(), movieLastCollected)) {
      queueTask(new SyncMoviesCollectionTask());
    }

    if (TraktTimestamps.movieWatchlistNeedsUpdate(getContext(), movieLastWatchlist)) {
      queueTask(new SyncMoviesWatchlistTask());
    }

    TraktTimestamps.update(getContext(), lastActivity);

    queueTask(new SyncWatchingTask());

    postOnSuccess();
  }
}
