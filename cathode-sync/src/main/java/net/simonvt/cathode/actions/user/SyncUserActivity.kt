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
package net.simonvt.cathode.actions.user

import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.api.entity.LastActivity
import net.simonvt.cathode.api.enumeration.ItemTypes
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.remote.sync.SyncHiddenItems
import net.simonvt.cathode.remote.sync.comments.SyncCommentLikes
import net.simonvt.cathode.remote.sync.comments.SyncUserComments
import net.simonvt.cathode.remote.sync.lists.SyncLists
import net.simonvt.cathode.remote.sync.movies.SyncMoviesCollection
import net.simonvt.cathode.remote.sync.movies.SyncMoviesRatings
import net.simonvt.cathode.remote.sync.movies.SyncMoviesWatchlist
import net.simonvt.cathode.remote.sync.movies.SyncWatchedMovies
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeWatchlist
import net.simonvt.cathode.remote.sync.shows.SyncEpisodesRatings
import net.simonvt.cathode.remote.sync.shows.SyncSeasonsRatings
import net.simonvt.cathode.remote.sync.shows.SyncShowsCollection
import net.simonvt.cathode.remote.sync.shows.SyncShowsRatings
import net.simonvt.cathode.remote.sync.shows.SyncShowsWatchlist
import net.simonvt.cathode.remote.sync.shows.SyncWatchedShows
import net.simonvt.cathode.settings.TraktTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncUserActivity @Inject constructor(
  private val context: Context,
  private val syncService: SyncService,
  private val jobManager: JobManager
) : CallAction<Unit, LastActivity>() {

  override fun getCall(params: Unit): Call<LastActivity> = syncService.lastActivity()

  override suspend fun handleResponse(params: Unit, response: LastActivity) {
    val showLastWatchlist = response.shows.watchlisted_at?.timeInMillis ?: 0L
    val showLastRating = response.shows.rated_at?.timeInMillis ?: 0L
    val showLastComment = response.shows.commented_at?.timeInMillis ?: 0L
    val showLastHide = response.shows.hidden_at?.timeInMillis ?: 0L

    val seasonLastRating = response.seasons.rated_at?.timeInMillis ?: 0L
    val seasonLastWatchlist = response.seasons.watchlisted_at?.timeInMillis ?: 0L
    val seasonLastComment = response.seasons.commented_at?.timeInMillis ?: 0L
    val seasonLastHide = response.seasons.hidden_at?.timeInMillis ?: 0L

    val episodeLastWatched = response.episodes.watched_at?.timeInMillis ?: 0L
    val episodeLastCollected = response.episodes.collected_at?.timeInMillis ?: 0L
    val episodeLastWatchlist = response.episodes.watchlisted_at?.timeInMillis ?: 0L
    val episodeLastRating = response.episodes.rated_at?.timeInMillis ?: 0L
    val episodeLastComment = response.episodes.commented_at?.timeInMillis ?: 0L

    val movieLastWatched = response.movies.watched_at?.timeInMillis ?: 0L
    val movieLastCollected = response.movies.collected_at?.timeInMillis ?: 0L
    val movieLastWatchlist = response.movies.watchlisted_at?.timeInMillis ?: 0L
    val movieLastRating = response.movies.rated_at?.timeInMillis ?: 0L
    val movieLastComment = response.movies.commented_at?.timeInMillis ?: 0L
    val movieLastHide = response.movies.hidden_at?.timeInMillis ?: 0L

    val commentLastLiked = response.comments.liked_at?.timeInMillis ?: 0L

    val listLastUpdated = response.lists.updated_at?.timeInMillis ?: 0L

    if (TraktTimestamps.episodeWatchedNeedsUpdate(context, episodeLastWatched)) {
      jobManager.addJob(SyncWatchedShows())
    }

    if (TraktTimestamps.episodeCollectedNeedsUpdate(context, episodeLastCollected)) {
      jobManager.addJob(SyncShowsCollection())
    }

    if (TraktTimestamps.episodeWatchlistNeedsUpdate(context, episodeLastWatchlist)) {
      jobManager.addJob(SyncEpisodeWatchlist())
    }

    if (TraktTimestamps.episodeRatingsNeedsUpdate(context, episodeLastRating)) {
      jobManager.addJob(SyncEpisodesRatings())
    }

    if (TraktTimestamps.episodeCommentsNeedsUpdate(context, episodeLastComment)) {
      jobManager.addJob(SyncUserComments(ItemTypes.EPISODES))
    }

    if (TraktTimestamps.seasonRatingsNeedsUpdate(context, seasonLastRating)) {
      jobManager.addJob(SyncSeasonsRatings())
    }

    if (TraktTimestamps.seasonCommentsNeedsUpdate(context, seasonLastComment)) {
      jobManager.addJob(SyncUserComments(ItemTypes.SEASONS))
    }

    if (TraktTimestamps.showWatchlistNeedsUpdate(context, showLastWatchlist)) {
      jobManager.addJob(SyncShowsWatchlist())
    }

    if (TraktTimestamps.showRatingsNeedsUpdate(context, showLastRating)) {
      jobManager.addJob(SyncShowsRatings())
    }

    if (TraktTimestamps.showCommentsNeedsUpdate(context, showLastComment)) {
      jobManager.addJob(SyncUserComments(ItemTypes.SHOWS))
    }

    if (TraktTimestamps.movieWatchedNeedsUpdate(context, movieLastWatched)) {
      jobManager.addJob(SyncWatchedMovies())
    }

    if (TraktTimestamps.movieCollectedNeedsUpdate(context, movieLastCollected)) {
      jobManager.addJob(SyncMoviesCollection())
    }

    if (TraktTimestamps.movieWatchlistNeedsUpdate(context, movieLastWatchlist)) {
      jobManager.addJob(SyncMoviesWatchlist())
    }

    if (TraktTimestamps.movieRatingsNeedsUpdate(context, movieLastRating)) {
      jobManager.addJob(SyncMoviesRatings())
    }

    if (TraktTimestamps.movieCommentsNeedsUpdate(context, movieLastComment)) {
      jobManager.addJob(SyncUserComments(ItemTypes.MOVIES))
    }

    if (TraktTimestamps.commentLikedNeedsUpdate(context, commentLastLiked)) {
      jobManager.addJob(SyncCommentLikes())
    }

    if (TraktTimestamps.listNeedsUpdate(context, listLastUpdated)) {
      jobManager.addJob(SyncLists())
    }

    if (TraktTimestamps.showHideNeedsUpdate(
        context,
        showLastHide
      ) || TraktTimestamps.movieHideNeedsUpdate(context, movieLastHide)
    ) {
      jobManager.addJob(SyncHiddenItems())
    }

    TraktTimestamps.update(context, response)
  }
}
