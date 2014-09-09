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

package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.body.RateItems;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.entity.CollectionItem;
import net.simonvt.cathode.api.entity.LastActivity;
import net.simonvt.cathode.api.entity.RatingItem;
import net.simonvt.cathode.api.entity.SyncResponse;
import net.simonvt.cathode.api.entity.WatchedItem;
import net.simonvt.cathode.api.entity.WatchlistItem;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

public interface SyncService {

  @GET("/sync/last_activities") LastActivity lastActivity();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get all collected shows in a user's collection. A collected item indicates availability to
   * watch digitally or on physical media.
   */
  @GET("/sync/collection/shows") List<CollectionItem> getShowCollection();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get all collected movies in a user's collection. A collected item indicates availability to
   * watch digitally or on physical media.
   */
  @GET("/sync/collection/movies") List<CollectionItem> getMovieCollection();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Add items to a user's collection. Accepts shows, seasons, episodes and movies. If only a show
   * is passed, all episodes for the show will be collected. If seasons are specified, all episodes
   * in those seasons will be collected.
   * <p>
   * Send a collected_at UTC datetime to mark items as collectedin the past. This is useful for
   * syncing collections from a media center.
   */
  @POST("/sync/collection") SyncResponse collect(@Body SyncItems collect);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Remove one or more items from a user's collection.
   */
  @POST("/sync/collection/remove") SyncResponse uncollect(@Body SyncItems collect);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Returns all movies a user has watched.
   */
  @GET("/sync/watched/movies") List<WatchedItem> getWatchedMovies();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Return all shows a user has watched.
   */
  @GET("/sync/watched/shows") List<WatchedItem> getWatchedShows();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Add items to a user's watch history. Accepts shows, seasons, episodes and movies. If only a
   * show is passed, all episodes for the show will be added. If seasons are specified, only
   * episodes in those seasons will be added.
   * <p>
   * Send a watched_at UTC datetime to mark items as watched in the past. This is useful for
   * syncing
   * past watches from a media center.
   */
  @POST("/sync/history") SyncResponse watched(@Body SyncItems collect);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Remove items from a user's watch history including all watches, scrobbles, and checkins.
   * Accepts shows, seasons, episodes and movies. If only a show is passed, all episodes for the
   * show will be removed. If seasons are specified, only episodes in those seasons will be
   * removed.
   */
  @POST("/sync/history/remove") SyncResponse unwatched(@Body SyncItems collect);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get a users ratings.
   */
  @GET("/sync/ratings/movies") List<RatingItem> getMovieRatings();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get a users ratings.
   */
  @GET("/sync/ratings/shows") List<RatingItem> getShowRatings();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get a users ratings.
   */
  @GET("/sync/ratings/seasons") List<RatingItem> getSeasonRatings();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get a users ratings.
   */
  @GET("/sync/ratings/episodes") List<RatingItem> getEpisodeRatings();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Rate one or more items. Accepts shows, seasons, episodes and movies. If only a show is passed,
   * only the show itself will be rated. If seasons are specified, all of those seasons will be
   * rated.
   * <p>
   * Send a rated_at UTC datetime to mark items as rated in the past. This is useful for syncing
   * ratings from a media center.
   */
  @POST("/sync/ratings") SyncResponse rate(@Body RateItems items);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Remove ratings for one or more items.
   */
  @POST("/sync/ratings/remove") SyncResponse removeRating(@Body RateItems items);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get a users movie watchlist. When a movie is watched, it will be automatically removed. To
   * track what the user is actively watching, use the progress APIs.
   */
  @GET("/sync/watchlist/movies") List<WatchlistItem> getMovieWatchlist();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get a users show watchlist. When a show is watched, it will be automatically removed. To
   * track what the user is actively watching, use the progress APIs.
   */
  @GET("/sync/watchlist/shows") List<WatchlistItem> getShowWatchlist();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get a users episode watchlist. When aan episode is watched, it will be automatically removed.
   * To track what the user is actively watching, use the progress APIs.
   */
  @GET("/sync/watchlist/episodes") List<WatchlistItem> getEpisodeWatchlist();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Add one of more items to a user's watchlist. Accepts shows, seasons, episodes and movies. If
   * only a show is passed, only the show itself will be added. If seasons are specified, all of
   * those seasons will be added.
   */
  @POST("/sync/watchlist") SyncResponse watchlist(@Body SyncItems collect);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Remove one or more items from a user's watchlist.
   */
  @POST("/sync/watchlist/remove") SyncResponse unwatchlist(@Body SyncItems collect);
}
