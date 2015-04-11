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
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesCollection;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesRatings;
import net.simonvt.cathode.remote.sync.movies.SyncMoviesWatchlist;
import net.simonvt.cathode.remote.sync.movies.SyncWatchedMovies;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeWatchlist;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodesRatings;
import net.simonvt.cathode.remote.sync.shows.SyncSeasonsRatings;
import net.simonvt.cathode.remote.sync.shows.SyncShowsCollection;
import net.simonvt.cathode.remote.sync.shows.SyncShowsRatings;
import net.simonvt.cathode.remote.sync.shows.SyncShowsWatchlist;
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows;
import net.simonvt.cathode.settings.TraktTimestamps;

public class SyncUserActivity extends Job {

  @Inject transient SyncService syncService;

  public SyncUserActivity() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncUserActivityTask";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public void perform() {
    LastActivity lastActivity = syncService.lastActivity();

    final long showLastWatchlist = lastActivity.getShows().getWatchlistedAt().getTimeInMillis();
    final long showLastRating = lastActivity.getShows().getRatedAt().getTimeInMillis();
    final long showLastComment = lastActivity.getShows().getCommentedAt().getTimeInMillis();

    final long seasonLastRating = lastActivity.getSeasons().getRatedAt().getTimeInMillis();
    final long seasonLastComment = lastActivity.getSeasons().getCommentedAt().getTimeInMillis();

    final long episodeLastWatched = lastActivity.getEpisodes().getWatchedAt().getTimeInMillis();
    final long episodeLastCollected = lastActivity.getEpisodes().getCollectedAt().getTimeInMillis();
    final long episodeLastWatchlist =
        lastActivity.getEpisodes().getWatchlistedAt().getTimeInMillis();
    final long episodeLastRating = lastActivity.getEpisodes().getRatedAt().getTimeInMillis();
    final long episodeLastComment = lastActivity.getEpisodes().getCommentedAt().getTimeInMillis();

    final long movieLastWatched = lastActivity.getMovies().getWatchedAt().getTimeInMillis();
    final long movieLastCollected = lastActivity.getMovies().getCollectedAt().getTimeInMillis();
    final long movieLastWatchlist = lastActivity.getMovies().getWatchlistedAt().getTimeInMillis();
    final long movieLastRating = lastActivity.getMovies().getRatedAt().getTimeInMillis();
    final long movieLastComment = lastActivity.getMovies().getCommentedAt().getTimeInMillis();

    if (TraktTimestamps.episodeWatchedNeedsUpdate(getContext(), episodeLastWatched)) {
      queue(new SyncWatchedShows());
    }

    if (TraktTimestamps.episodeCollectedNeedsUpdate(getContext(), episodeLastCollected)) {
      queue(new SyncShowsCollection());
    }

    if (TraktTimestamps.episodeWatchlistNeedsUpdate(getContext(), episodeLastWatchlist)) {
      queue(new SyncEpisodeWatchlist());
    }

    if (TraktTimestamps.episodeRatingsNeedsUpdate(getContext(), episodeLastRating)) {
      queue(new SyncEpisodesRatings());
    }

    if (TraktTimestamps.episodeCommentsNeedsUpdate(getContext(), episodeLastComment)) {
      // TODO: Handle comments eventually
    }

    if (TraktTimestamps.seasonRatingsNeedsUpdate(getContext(), seasonLastRating)) {
      queue(new SyncSeasonsRatings());
    }

    if (TraktTimestamps.seasonCommentsNeedsUpdate(getContext(), seasonLastComment)) {
      // TODO: Handle comments eventually
    }

    if (TraktTimestamps.showWatchlistNeedsUpdate(getContext(), showLastWatchlist)) {
      queue(new SyncShowsWatchlist());
    }

    if (TraktTimestamps.showRatingsNeedsUpdate(getContext(), showLastRating)) {
      queue(new SyncShowsRatings());
    }

    if (TraktTimestamps.showCommentsNeedsUpdate(getContext(), showLastComment)) {
      // TODO: Handle comments eventually
    }

    if (TraktTimestamps.movieWatchedNeedsUpdate(getContext(), movieLastWatched)) {
      queue(new SyncWatchedMovies());
    }

    if (TraktTimestamps.movieCollectedNeedsUpdate(getContext(), movieLastCollected)) {
      queue(new SyncMoviesCollection());
    }

    if (TraktTimestamps.movieWatchlistNeedsUpdate(getContext(), movieLastWatchlist)) {
      queue(new SyncMoviesWatchlist());
    }

    if (TraktTimestamps.movieRatingsNeedsUpdate(getContext(), movieLastRating)) {
      queue(new SyncMoviesRatings());
    }

    if (TraktTimestamps.movieCommentsNeedsUpdate(getContext(), movieLastComment)) {
      // TODO: Handle comments eventually
    }

    queue(new SyncWatching());

    TraktTimestamps.update(getContext(), lastActivity);
  }
}
