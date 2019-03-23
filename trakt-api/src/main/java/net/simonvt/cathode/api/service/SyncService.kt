/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.service

import net.simonvt.cathode.api.body.RateItems
import net.simonvt.cathode.api.body.RemoveHistoryBody
import net.simonvt.cathode.api.body.SyncItems
import net.simonvt.cathode.api.entity.CollectionItem
import net.simonvt.cathode.api.entity.HistoryItem
import net.simonvt.cathode.api.entity.LastActivity
import net.simonvt.cathode.api.entity.RatingItem
import net.simonvt.cathode.api.entity.SyncResponse
import net.simonvt.cathode.api.entity.WatchedItem
import net.simonvt.cathode.api.entity.WatchlistItem
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SyncService {

  /**
   * **OAuth Required**
   *
   *
   * Get all collected shows in a user's collection. A collected item indicates availability to
   * watch digitally or on physical media.
   */
  @GET("/sync/collection/shows")
  fun getShowCollection(): Call<List<CollectionItem>>

  /**
   * **OAuth Required**
   *
   *
   * Get all collected movies in a user's collection. A collected item indicates availability to
   * watch digitally or on physical media.
   */
  @GET("/sync/collection/movies")
  fun getMovieCollection(): Call<List<CollectionItem>>

  /**
   * **OAuth Required**
   *
   *
   * Returns all movies a user has watched.
   */
  @GET("/sync/watched/movies")
  fun getWatchedMovies(): Call<List<WatchedItem>>

  /**
   * **OAuth Required**
   *
   *
   * Return all shows a user has watched.
   */
  @GET("/sync/watched/shows")
  fun getWatchedShows(): Call<List<WatchedItem>>

  /**
   * **OAuth Required**
   *
   *
   * Get a users ratings.
   */
  @GET("/sync/ratings/movies")
  fun getMovieRatings(): Call<List<RatingItem>>

  /**
   * **OAuth Required**
   *
   *
   * Get a users ratings.
   */
  @GET("/sync/ratings/shows")
  fun getShowRatings(): Call<List<RatingItem>>

  /**
   * **OAuth Required**
   *
   *
   * Get a users ratings.
   */
  @GET("/sync/ratings/seasons")
  fun getSeasonRatings(): Call<List<RatingItem>>

  /**
   * **OAuth Required**
   *
   *
   * Get a users ratings.
   */
  @GET("/sync/ratings/episodes")
  fun getEpisodeRatings(): Call<List<RatingItem>>

  /**
   * **OAuth Required**
   *
   *
   * Get a users movie watchlist. When a movie is watched, it will be automatically removed. To
   * track what the user is actively watching, use the progress APIs.
   */
  @GET("/sync/watchlist/movies")
  fun getMovieWatchlist(): Call<List<WatchlistItem>>

  /**
   * **OAuth Required**
   *
   *
   * Get a users show watchlist. When a show is watched, it will be automatically removed. To
   * track what the user is actively watching, use the progress APIs.
   */
  @GET("/sync/watchlist/shows")
  fun getShowWatchlist(): Call<List<WatchlistItem>>

  /**
   * **OAuth Required**
   *
   *
   * Get a users episode watchlist. When aan episode is watched, it will be automatically removed.
   * To track what the user is actively watching, use the progress APIs.
   */
  @GET("/sync/watchlist/episodes")
  fun getEpisodeWatchlist(): Call<List<WatchlistItem>>

  @GET("/sync/last_activities")
  fun lastActivity(): Call<LastActivity>

  /**
   * **OAuth Required**
   *
   *
   * Add items to a user's collection. Accepts shows, seasons, episodes and movies. If only a show
   * is passed, all episodes for the show will be collected. If seasons are specified, all episodes
   * in those seasons will be collected.
   *
   *
   * Send a collected_at UTC datetime to mark items as collectedin the past. This is useful for
   * syncing collections from a media center.
   */
  @POST("/sync/collection")
  fun collect(@Body collect: SyncItems): Call<SyncResponse>

  /**
   * **OAuth Required**
   *
   *
   * Remove one or more items from a user's collection.
   */
  @POST("/sync/collection/remove")
  fun uncollect(@Body collect: SyncItems): Call<SyncResponse>

  /**
   * **OAuth Required**
   *
   *
   * Return watched history for episodes the user has watched, sorted by most recent.
   */
  @GET("/sync/history/shows/{id}")
  fun getShowHistory(@Path("id") id: Long): Call<List<HistoryItem>>

  /**
   * **OAuth Required**
   *
   *
   * Return watched history for episodes the user has watched, sorted by most recent.
   */
  @GET("/sync/history/seasons/{id}")
  fun getSeasonHistory(@Path("id") id: Long): Call<List<HistoryItem>>

  /**
   * **OAuth Required**
   *
   *
   * Return watched history for an episode, sorted by most recent.
   */
  @GET("/sync/history/episodes/{id}")
  fun getEpisodeHistory(
    @Path("id") id: Long
  ): Call<List<HistoryItem>>

  /**
   * **OAuth Required**
   *
   *
   * Return watched history for a movie, sorted by most recent.
   */
  @GET("/sync/history/movies/{id}")
  fun getMovieHistory(@Path("id") id: Long): Call<List<HistoryItem>>

  /**
   * **OAuth Required**
   *
   *
   * Remove items from a user's watch history including all watches, scrobbles, and checkins.
   * Accepts shows, seasons, episodes and movies. If only a show is passed, all episodes for the
   * show will be removed. If seasons are specified, only episodes in those seasons will be
   * removed.
   *
   *
   * You can also send a list of raw history ids (64-bit integers) to delete single plays
   * from the watched history. The /sync/history method will return an individual id (64-bit
   * integer) for each history item.
   */
  @POST("/sync/history/remove")
  fun removeHistory(@Body body: RemoveHistoryBody): Call<SyncResponse>

  /**
   * **OAuth Required**
   *
   *
   * Add items to a user's watch history. Accepts shows, seasons, episodes and movies. If only a
   * show is passed, all episodes for the show will be added. If seasons are specified, only
   * episodes in those seasons will be added.
   *
   *
   * Send a watched_at UTC datetime to mark items as watched in the past. This is useful for
   * syncing
   * past watches from a media center.
   */
  @POST("/sync/history")
  fun watched(@Body watch: SyncItems): Call<SyncResponse>

  /**
   * **OAuth Required**
   *
   *
   * Remove items from a user's watch history including all watches, scrobbles, and checkins.
   * Accepts shows, seasons, episodes and movies. If only a show is passed, all episodes for the
   * show will be removed. If seasons are specified, only episodes in those seasons will be
   * removed.
   */
  @POST("/sync/history/remove")
  fun unwatched(@Body collect: SyncItems): Call<SyncResponse>

  /**
   * **OAuth Required**
   *
   *
   * Rate one or more items. Accepts shows, seasons, episodes and movies. If only a show is passed,
   * only the show itself will be rated. If seasons are specified, all of those seasons will be
   * rated.
   *
   *
   * Send a rated_at UTC datetime to mark items as rated in the past. This is useful for syncing
   * ratings from a media center.
   */
  @POST("/sync/ratings")
  fun rate(@Body items: RateItems): Call<SyncResponse>

  /**
   * **OAuth Required**
   *
   *
   * Remove ratings for one or more items.
   */
  @POST("/sync/ratings/remove")
  fun removeRating(@Body items: RateItems): Call<SyncResponse>

  /**
   * **OAuth Required**
   *
   *
   * Add one of more items to a user's watchlist. Accepts shows, seasons, episodes and movies. If
   * only a show is passed, only the show itself will be added. If seasons are specified, all of
   * those seasons will be added.
   */
  @POST("/sync/watchlist")
  fun watchlist(@Body collect: SyncItems): Call<SyncResponse>

  /**
   * **OAuth Required**
   *
   *
   * Remove one or more items from a user's watchlist.
   */
  @POST("/sync/watchlist/remove")
  fun unwatchlist(@Body collect: SyncItems): Call<SyncResponse>
}
