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

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.LastActivity;
import net.simonvt.cathode.api.enumeration.ItemTypes;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.jobscheduler.SchedulerService;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.comments.SyncCommentLikes;
import net.simonvt.cathode.remote.sync.comments.SyncUserComments;
import net.simonvt.cathode.remote.sync.lists.SyncLists;
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
import retrofit2.Call;

public class SyncUserActivity extends CallJob<LastActivity> {

  public static final int ID = 102;

  @Inject transient SyncService syncService;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public static void schedulePeriodic(Context context) {
    JobInfo jobInfo = new JobInfo.Builder(ID, new ComponentName(context, SchedulerService.class)) //
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setRequiresCharging(true)
        .setRequiresDeviceIdle(true)
        .setPeriodic(android.text.format.DateUtils.DAY_IN_MILLIS)
        .setPersisted(true)
        .build();
    Jobs.scheduleNotPending(context, jobInfo);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) public static void cancel(Context context) {
    Jobs.cancel(context, ID);
  }

  public SyncUserActivity() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncUserActivity";
  }

  @Override public int getPriority() {
    return JobPriority.USER_DATA;
  }

  @Override public Call<LastActivity> getCall() {
    return syncService.lastActivity();
  }

  @Override public boolean handleResponse(LastActivity lastActivity) {
    final long showLastWatchlist = lastActivity.getShows().getWatchlistedAt().getTimeInMillis();
    final long showLastRating = lastActivity.getShows().getRatedAt().getTimeInMillis();
    final long showLastComment = lastActivity.getShows().getCommentedAt().getTimeInMillis();
    final long showLastHide = lastActivity.getShows().getHiddenAt().getTimeInMillis();

    final long seasonLastRating = lastActivity.getSeasons().getRatedAt().getTimeInMillis();
    final long seasonLastWatchlist = lastActivity.getSeasons().getWatchlistedAt().getTimeInMillis();
    final long seasonLastComment = lastActivity.getSeasons().getCommentedAt().getTimeInMillis();
    final long seasonLastHide = lastActivity.getSeasons().getHiddenAt().getTimeInMillis();

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
    final long movieLastHide = lastActivity.getMovies().getHiddenAt().getTimeInMillis();

    final long commentLastLiked = lastActivity.getComments().getLikedAt().getTimeInMillis();

    final long listLastUpdated = lastActivity.getLists().getUpdatedAt().getTimeInMillis();

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
      queue(new SyncUserComments(ItemTypes.EPISODES));
    }

    if (TraktTimestamps.seasonRatingsNeedsUpdate(getContext(), seasonLastRating)) {
      queue(new SyncSeasonsRatings());
    }

    if (TraktTimestamps.seasonCommentsNeedsUpdate(getContext(), seasonLastComment)) {
      queue(new SyncUserComments(ItemTypes.SEASONS));
    }

    if (TraktTimestamps.showWatchlistNeedsUpdate(getContext(), showLastWatchlist)) {
      queue(new SyncShowsWatchlist());
    }

    if (TraktTimestamps.showRatingsNeedsUpdate(getContext(), showLastRating)) {
      queue(new SyncShowsRatings());
    }

    if (TraktTimestamps.showCommentsNeedsUpdate(getContext(), showLastComment)) {
      queue(new SyncUserComments(ItemTypes.SHOWS));
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
      queue(new SyncUserComments(ItemTypes.MOVIES));
    }

    if (TraktTimestamps.commentLikedNeedsUpdate(getContext(), commentLastLiked)) {
      queue(new SyncCommentLikes());
    }

    if (TraktTimestamps.listNeedsUpdate(getContext(), listLastUpdated)) {
      queue(new SyncLists());
    }

    if (TraktTimestamps.showHideNeedsUpdate(getContext(), showLastHide)
        || TraktTimestamps.movieHideNeedsUpdate(getContext(), movieLastHide)) {
      queue(new SyncHiddenItems());
    }

    TraktTimestamps.update(getContext(), lastActivity);

    return true;
  }
}
