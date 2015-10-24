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
import net.simonvt.cathode.api.body.CreateListBody;
import net.simonvt.cathode.api.body.ListItemActionBody;
import net.simonvt.cathode.api.entity.CommentItem;
import net.simonvt.cathode.api.entity.CustomList;
import net.simonvt.cathode.api.entity.HiddenItem;
import net.simonvt.cathode.api.entity.Like;
import net.simonvt.cathode.api.entity.ListItem;
import net.simonvt.cathode.api.entity.ListItemActionResponse;
import net.simonvt.cathode.api.entity.Profile;
import net.simonvt.cathode.api.entity.UserSettings;
import net.simonvt.cathode.api.entity.Watching;
import net.simonvt.cathode.api.enumeration.CommentType;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.enumeration.HiddenSection;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.enumeration.ItemTypes;
import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public interface UsersService {

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get the user's settings so you can align your app's experience with what they're used to on
   * the trakt website.
   */
  @GET("/users/settings") Call<UserSettings> getUserSettings();

  /**
   * <b>OAuth Optional</b>
   * <p>
   * Get a user's profile information. If the user is private, info will only be returned if you
   * send OAuth and are either that user or an approved follower.
   */
  @GET("/users/me") Call<Profile> getProfile(@Query("extended") Extended extended);

  /**
   * <b>OAuth Required</b>
   * <b>Pagination</b>
   * <p>
   * Get hidden items for a section. This will return an array of standard media objects.
   * You can optionally limit the type of results to return.
   */
  @GET("/users/hidden/{section}") Call<List<HiddenItem>> getHiddenItems(
      @Path("section") HiddenSection section, @Query("page") int page, @Query("limit") int limit);

  /**
   * <b>OAuth Required</b>
   * <b>Pagination</b>
   * <p>
   * Get hidden items for a section. This will return an array of standard media objects.
   * You can optionally limit the type of results to return.
   *
   * @param type Possible values: {@link ItemType#SHOW}, {@link ItemType#SEASON},
   * {@link ItemType#MOVIE}
   */
  @GET("/users/hidden/{section}") Call<List<HiddenItem>> getHiddenItems(
      @Path("section") HiddenSection section, @Query("type") ItemType type, @Query("page") int page,
      @Query("limit") int limit);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Returns all movies or shows a user has watched sorted by most plays.
   */
  @GET("/users/me/watching") Call<Watching> watching();

  /**
   * <b>OAuth Optional</b>
   * <p>
   * Returns all movies or shows a user has watched sorted by most plays.
   */
  @GET("/users/{username}/watching") Call<Watching> watching(@Path("username") String username);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Returns all custom lists for a user.
   */
  @GET("/users/me/lists") Call<List<CustomList>> lists();

  /**
   * <b>OAuth Optional</b>
   * <p>
   * Returns all custom lists for a user.
   */
  @GET("/users/{username}/lists") Call<List<CustomList>> lists(@Path("username") String username);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get all items on a custom list. Items can be movies, shows, seasons, episodes, or people.
   */
  @GET("/users/me/lists/{id}/items") Call<List<ListItem>> listItems(@Path("id") long id);

  /**
   * <b>OAuth Optional</b>
   * <p>
   * Get all items on a custom list. Items can be movies, shows, seasons, episodes, or people.
   */
  @GET("/users/{username}/lists/{id}/items") Call<List<ListItem>> listItems(
      @Path("username") String username, @Path("id") long id);

  @POST("/users/me/lists") Call<CustomList> createList(@Body CreateListBody createList);

  @POST("/users/{username}/lists") Call<CustomList> createList(@Path("username") String username,
      @Body CreateListBody createList);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Add one or more items to a custom list. Items can be movies, shows, seasons, episodes,
   * or people.
   */
  @POST("/users/me/lists/{id}/items") Call<ListItemActionResponse> addItems(@Path("id") long id,
      @Body ListItemActionBody item);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Add one or more items to a custom list. Items can be movies, shows, seasons, episodes,
   * or people.
   */
  @POST("/users/{username}/lists/{id}/items") Call<ListItemActionResponse> addItems(
      @Path("username") String username, @Path("id") long id, @Body ListItemActionBody item);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Remove one or more items from a custom list.
   */
  @POST("/users/me/lists/{id}/items/remove") Call<ListItemActionResponse> removeItem(
      @Path("id") long id, @Body ListItemActionBody item);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Remove one or more items from a custom list.
   */
  @POST("/users/{username}/lists/{id}/items/remove") Call<ListItemActionResponse> removeItem(
      @Path("username") String username, @Path("id") long id, @Body ListItemActionBody item);

  /**
   * <b>OAuth Required</b>
   * <b>Pagination</b>
   * <p>
   * Returns comments a user has posted sorted by most recent.
   */
  @GET("/users/me/comments/{comment_type}/{type}") Call<List<CommentItem>> getUserComments(
      @Path("comment_type") CommentType commentType, @Path("type") ItemTypes itemTypes,
      @Query("page") int page, @Query("limit") int limit);

  /**
   * <b>OAuth Required</b>
   * <b>Pagination</b>
   * <p>
   * Get items a user likes. This will return an array of standard media objects.
   * @param itemTypes One of {@link ItemTypes#COMMENTS} and {@link ItemTypes#LISTS}.
   */
  @GET("/users/likes/{type}") Call<List<Like>> getLikes(@Path("type") ItemTypes itemTypes,
      @Query("page") int page, @Query("limit") int limit);
}
