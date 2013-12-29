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
import net.simonvt.cathode.settings.ActivityWrapper;
import retrofit.RetrofitError;

public class SyncUserActivityTask extends TraktTask {

  @Inject transient UserService userService;

  @Override protected void doTask() {
    try {
      LastActivity lastActivity = userService.lastActivity();

      long episodeLastWatched = lastActivity.getEpisode().getWatched();
      long episodeLastCollected = lastActivity.getEpisode().getCollection();
      long episodeLastWatchlist = lastActivity.getEpisode().getWatchlist();

      long showLastWatchlist = lastActivity.getShow().getWatchlist();

      long movieLastWatched = lastActivity.getMovie().getWatched();
      long movieLastCollected = lastActivity.getMovie().getCollection();
      long movieLastWatchlist = lastActivity.getMovie().getWatchlist();

      if (ActivityWrapper.episodeWatchedNeedsUpdate(service, episodeLastWatched)) {
        queueTask(new SyncShowsWatchedTask());
      }

      if (ActivityWrapper.episodeCollectedNeedsUpdate(service, episodeLastCollected)) {
        queueTask(new SyncShowsCollectionTask());
      }

      if (ActivityWrapper.episodeWatchlistNeedsUpdate(service, episodeLastWatchlist)) {
        queueTask(new SyncEpisodeWatchlistTask());
      }

      if (ActivityWrapper.showWatchlistNeedsUpdate(service, showLastWatchlist)) {
        queueTask(new SyncShowsWatchlistTask());
      }

      if (ActivityWrapper.movieWatchedNeedsUpdate(service, movieLastWatched)) {
        queueTask(new SyncMoviesWatchedTask());
      }

      if (ActivityWrapper.movieCollectedNeedsUpdate(service, movieLastCollected)) {
        queueTask(new SyncMoviesCollectionTask());
      }

      if (ActivityWrapper.movieWatchlistNeedsUpdate(service, movieLastWatchlist)) {
        queueTask(new SyncMoviesWatchlistTask());
      }

      ActivityWrapper.update(service, lastActivity);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
