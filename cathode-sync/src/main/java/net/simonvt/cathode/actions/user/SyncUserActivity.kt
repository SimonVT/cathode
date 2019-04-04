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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.api.entity.LastActivity
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.settings.TraktTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncUserActivity @Inject constructor(
  private val context: Context,
  private val syncService: SyncService,
  private val jobManager: JobManager,
  private val syncWatchedShows: SyncWatchedShows,
  private val syncShowsCollection: SyncShowsCollection,
  private val syncEpisodeWatchlist: SyncEpisodeWatchlist,
  private val syncEpisodesRatings: SyncEpisodesRatings,
  private val syncUserEpisodeComments: SyncUserEpisodeComments,
  private val syncSeasonsRatings: SyncSeasonsRatings,
  private val syncUserSeasonComments: SyncUserSeasonComments,
  private val syncShowsWatchlist: SyncShowsWatchlist,
  private val syncShowsRatings: SyncShowsRatings,
  private val syncUserShowComments: SyncUserShowComments,
  private val syncWatchedMovies: SyncWatchedMovies,
  private val syncMoviesCollection: SyncMoviesCollection,
  private val syncMoviesWatchlist: SyncMoviesWatchlist,
  private val syncMoviesRatings: SyncMoviesRatings,
  private val syncUserMovieComments: SyncUserMovieComments,
  private val syncCommentLikes: SyncCommentLikes,
  private val syncLists: SyncLists,
  private val syncHiddenItems: SyncHiddenItems
) : CallAction<Unit, LastActivity>() {

  override fun key(params: Unit): String = "SyncUserActivity"

  override fun getCall(params: Unit): Call<LastActivity> = syncService.lastActivity()

  override suspend fun handleResponse(params: Unit, response: LastActivity) = coroutineScope {
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

    val updates = mutableListOf<Deferred<*>>()

    if (TraktTimestamps.episodeWatchedNeedsUpdate(context, episodeLastWatched)) {
      updates += syncWatchedShows.invokeAsync(SyncWatchedShows.Params(episodeLastWatched))
    }

    if (TraktTimestamps.episodeCollectedNeedsUpdate(context, episodeLastCollected)) {
      updates += syncShowsCollection.invokeAsync(SyncShowsCollection.Params(episodeLastCollected))
    }

    if (TraktTimestamps.episodeWatchlistNeedsUpdate(context, episodeLastWatchlist)) {
      updates += syncEpisodeWatchlist.invokeAsync(SyncEpisodeWatchlist.Params(episodeLastWatchlist))
    }

    if (TraktTimestamps.episodeRatingsNeedsUpdate(context, episodeLastRating)) {
      updates += syncEpisodesRatings.invokeAsync(SyncEpisodesRatings.Params(episodeLastRating))
    }

    if (TraktTimestamps.episodeCommentsNeedsUpdate(context, episodeLastComment)) {
      updates += syncUserEpisodeComments.invokeAsync(
        SyncUserEpisodeComments.Params(
          episodeLastComment
        )
      )
    }

    if (TraktTimestamps.seasonRatingsNeedsUpdate(context, seasonLastRating)) {
      updates += syncSeasonsRatings.invokeAsync(SyncSeasonsRatings.Params(seasonLastRating))
    }

    if (TraktTimestamps.seasonCommentsNeedsUpdate(context, seasonLastComment)) {
      updates += syncUserSeasonComments.invokeAsync(SyncUserSeasonComments.Params(seasonLastComment))
    }

    if (TraktTimestamps.showWatchlistNeedsUpdate(context, showLastWatchlist)) {
      updates += syncShowsWatchlist.invokeAsync(SyncShowsWatchlist.Params(showLastWatchlist))
    }

    if (TraktTimestamps.showRatingsNeedsUpdate(context, showLastRating)) {
      updates += syncShowsRatings.invokeAsync(SyncShowsRatings.Params(showLastRating))
    }

    if (TraktTimestamps.showCommentsNeedsUpdate(context, showLastComment)) {
      updates += syncUserShowComments.invokeAsync(SyncUserShowComments.Params(showLastComment))
    }

    if (TraktTimestamps.movieWatchedNeedsUpdate(context, movieLastWatched)) {
      updates += syncWatchedMovies.invokeAsync(SyncWatchedMovies.Params(movieLastWatched))
    }

    if (TraktTimestamps.movieCollectedNeedsUpdate(context, movieLastCollected)) {
      updates += syncMoviesCollection.invokeAsync(SyncMoviesCollection.Params(movieLastCollected))
    }

    if (TraktTimestamps.movieWatchlistNeedsUpdate(context, movieLastWatchlist)) {
      updates += syncMoviesWatchlist.invokeAsync(SyncMoviesWatchlist.Params(movieLastWatchlist))
    }

    if (TraktTimestamps.movieRatingsNeedsUpdate(context, movieLastRating)) {
      updates += syncMoviesRatings.invokeAsync(SyncMoviesRatings.Params(movieLastRating))
    }

    if (TraktTimestamps.movieCommentsNeedsUpdate(context, movieLastComment)) {
      updates += syncUserMovieComments.invokeAsync(SyncUserMovieComments.Params(movieLastComment))
    }

    if (TraktTimestamps.commentLikedNeedsUpdate(context, commentLastLiked)) {
      updates += syncCommentLikes.invokeAsync(SyncCommentLikes.Params(commentLastLiked))
    }

    if (TraktTimestamps.listNeedsUpdate(context, listLastUpdated)) {
      updates += syncLists.invokeAsync(SyncLists.Params(listLastUpdated))
    }

    if (
      TraktTimestamps.showHideNeedsUpdate(context, showLastHide) ||
      TraktTimestamps.movieHideNeedsUpdate(context, movieLastHide)
    ) {
      updates += syncHiddenItems.invokeAsync(SyncHiddenItems.Params(showLastHide, movieLastHide))
    }

    updates.forEach { it.await() }
  }
}
