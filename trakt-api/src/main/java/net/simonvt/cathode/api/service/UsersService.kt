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

import net.simonvt.cathode.api.body.HiddenItems
import net.simonvt.cathode.api.body.IdsBody
import net.simonvt.cathode.api.body.ListInfoBody
import net.simonvt.cathode.api.entity.CommentItem
import net.simonvt.cathode.api.entity.CustomList
import net.simonvt.cathode.api.entity.HiddenItem
import net.simonvt.cathode.api.entity.HideResponse
import net.simonvt.cathode.api.entity.Like
import net.simonvt.cathode.api.entity.ListItem
import net.simonvt.cathode.api.entity.ListItemActionResponse
import net.simonvt.cathode.api.entity.Profile
import net.simonvt.cathode.api.entity.UserSettings
import net.simonvt.cathode.api.entity.Watching
import net.simonvt.cathode.api.enumeration.CommentType
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.enumeration.HiddenSection
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.enumeration.ItemTypes
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UsersService {

  /**
   * **OAuth Required**
   *
   *
   * Get the user's settings so you can align your app's experience with what they're used to on
   * the trakt website.
   */
  @GET("/users/settings")
  fun getUserSettings(): Call<UserSettings>

  /**
   * **OAuth Optional**
   *
   *
   * Get a user's profile information. If the user is private, info will only be returned if you
   * send OAuth and are either that user or an approved follower.
   */
  @GET("/users/me")
  fun getProfile(@Query("extended") extended: Extended? = null): Call<Profile>

  /**
   * **OAuth Required**
   * **Pagination**
   *
   *
   * Get hidden items for a section. This will return an array of standard media objects.
   * You can optionally limit the type of results to return.
   *
   * @param type Possible values: [ItemType.SHOW], [ItemType.SEASON],
   * [ItemType.MOVIE]
   */
  @GET("/users/hidden/{section}")
  fun getHiddenItems(
    @Path("section") section: HiddenSection,
    @Query("type") type: ItemType? = null,
    @Query("page") page: Int,
    @Query("limit") limit: Int = LIMIT
  ): Call<List<HiddenItem>>

  /**
   * **OAuth Required**
   *
   *
   * Hide items for a specific section.
   */
  @POST("/users/hidden/{section}")
  fun addHiddenItems(@Path("section") section: HiddenSection, @Body hiddenItems: HiddenItems): Call<HideResponse>

  /**
   * **OAuth Required**
   *
   *
   * Unhide items for a specific section.
   */
  @POST("/users/hidden/{section}/remove")
  fun removeHiddenItems(@Path("section") section: HiddenSection, @Body hiddenItems: HiddenItems): Call<HideResponse>

  /**
   * **OAuth Required**
   *
   *
   * Returns all movies or shows a user has watched sorted by most plays.
   */
  @GET("/users/me/watching")
  fun watching(): Call<Watching>

  /**
   * **OAuth Optional**
   *
   *
   * Returns all movies or shows a user has watched sorted by most plays.
   */
  @GET("/users/{username}/watching")
  fun watching(@Path("username") username: String): Call<Watching>

  /**
   * **OAuth Required**
   *
   *
   * Returns all custom lists for a user.
   */
  @GET("/users/me/lists")
  fun lists(): Call<List<CustomList>>

  /**
   * **OAuth Optional**
   *
   *
   * Returns all custom lists for a user.
   */
  @GET("/users/{username}/lists")
  fun lists(@Path("username") username: String): Call<List<CustomList>>

  /**
   * **OAuth Required**
   *
   *
   * Get all items on a custom list. Items can be movies, shows, seasons, episodes, or people.
   */
  @GET("/users/me/lists/{id}/items")
  fun listItems(@Path("id") id: Long): Call<List<ListItem>>

  /**
   * **OAuth Optional**
   *
   *
   * Get all items on a custom list. Items can be movies, shows, seasons, episodes, or people.
   */
  @GET("/users/{username}/lists/{id}/items")
  fun listItems(@Path("username") username: String, @Path("id") id: Long): Call<List<ListItem>>

  @POST("/users/me/lists")
  fun createList(@Body createList: ListInfoBody): Call<CustomList>

  /**
   * **OAuth Required**
   *
   *
   * Update a custom list by sending 1 or more parameters. If you update the list name, the original
   * slug will still be retained so existing references to this list won't break.
   */
  @PUT("/users/me/lists/{id}")
  fun updateList(
    @Path("id") id: Long,
    @Body updateList: ListInfoBody
  ): Call<CustomList>

  /**
   * **OAuth Required**
   *
   *
   * Remove a custom list and all items it contains.
   */
  @DELETE("/users/me/lists/{id}")
  fun deleteList(@Path("id") id: Long): Call<ResponseBody>

  /**
   * **OAuth Required**
   *
   *
   * Add one or more items to a custom list. Items can be movies, shows, seasons, episodes,
   * or people.
   */
  @POST("/users/me/lists/{id}/items")
  fun addItems(@Path("id") id: Long, @Body item: IdsBody): Call<ListItemActionResponse>

  /**
   * **OAuth Required**
   *
   *
   * Add one or more items to a custom list. Items can be movies, shows, seasons, episodes,
   * or people.
   */
  @POST("/users/{username}/lists/{id}/items")
  fun addItems(
    @Path("username") username: String,
    @Path("id") id: Long,
    @Body item: IdsBody
  ): Call<ListItemActionResponse>

  /**
   * **OAuth Required**
   *
   *
   * Remove one or more items from a custom list.
   */
  @POST("/users/me/lists/{id}/items/remove")
  fun removeItem(@Path("id") id: Long, @Body item: IdsBody): Call<ListItemActionResponse>

  /**
   * **OAuth Required**
   *
   *
   * Remove one or more items from a custom list.
   */
  @POST("/users/{username}/lists/{id}/items/remove")
  fun removeItem(
    @Path("username") username: String,
    @Path("id") id: Long,
    @Body item: IdsBody
  ): Call<ListItemActionResponse>

  /**
   * **OAuth Required**
   * **Pagination**
   *
   *
   * Returns comments a user has posted sorted by most recent.
   */
  @GET("/users/me/comments/{comment_type}/{type}")
  fun getUserComments(
    @Path("comment_type") commentType: CommentType,
    @Path("type") itemTypes: ItemTypes,
    @Query("page") page: Int,
    @Query("limit") limit: Int = LIMIT
  ): Call<List<CommentItem>>

  /**
   * **OAuth Required**
   * **Pagination**
   *
   *
   * Get items a user likes. This will return an array of standard media objects.
   *
   * @param itemTypes One of [ItemTypes.COMMENTS] and [ItemTypes.LISTS].
   */
  @GET("/users/likes/{type}")
  fun getLikes(
    @Path("type") itemTypes: ItemTypes,
    @Query("page") page: Int,
    @Query("limit") limit: Int = LIMIT
  ): Call<List<Like>>

  companion object {
    const val ME = "me"
    const val LIMIT = 100
  }
}
