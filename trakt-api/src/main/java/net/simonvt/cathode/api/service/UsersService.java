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
import net.simonvt.cathode.api.entity.CustomList;
import net.simonvt.cathode.api.entity.ListItem;
import net.simonvt.cathode.api.entity.UserSettings;
import net.simonvt.cathode.api.entity.Watching;
import retrofit.http.GET;
import retrofit.http.Path;

public interface UsersService {

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get the user's settings so you can align your app's experience with what they're used to on
   * the trakt website.
   */
  @GET("/users/settings") UserSettings getUserSettings();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Returns all movies or shows a user has watched sorted by most plays.
   */
  @GET("/users/{username}/watching") Watching watching();

  /**
   * <b>OAuth Optional</b>
   * <p>
   * Returns all movies or shows a user has watched sorted by most plays.
   */
  @GET("/users/{username}/watching") Watching watching(@Path("username") String username);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Returns all custom lists for a user.
   */
  @GET("/users/{username}/lists") List<CustomList> lists();

  /**
   * <b>OAuth Optional</b>
   * <p>
   * Returns all custom lists for a user.
   */
  @GET("/users/{username}/lists") List<CustomList> lists(@Path("username") String username);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Get all items on a custom list. Items can be movies, shows, seasons, episodes, or people.
   */
  @GET("/users/{username}/lists/{id}/items") List<ListItem> listItems(@Path("id") long id);

  /**
   * <b>OAuth Optional</b>
   * <p>
   * Get all items on a custom list. Items can be movies, shows, seasons, episodes, or people.
   */
  @GET("/users/{username}/lists/{id}/items") List<ListItem> listItems(
      @Path("username") String username, @Path("id") long id);
}
