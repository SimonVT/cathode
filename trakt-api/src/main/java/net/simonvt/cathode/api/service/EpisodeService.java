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
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.enumeration.Extended;
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public interface EpisodeService {

  /**
   * Returns a single episode's details.
   *
   * @param showId Trakt ID.
   * @param season Season number Example: 1.
   * @param episode Episode number Example: 1.
   */
  @GET("/shows/{id}/seasons/{season}/episodes/{episode}") Call<Episode> getSummary(
      @Path("id") long showId, @Path("season") Integer season, @Path("episode") Integer episode,
      @Query("extended") Extended extended);

  /**
   * <b>Pagination</b>
   * <p>
   * Returns all top level comments for a movie. Most recent comments returned first.
   */
  @GET("/movies/{id}/comments") Call<List<Comment>> getComments(@Path("id") long showId,
      @Path("season") int season, @Path("episode") int episode, @Query("page") int page,
      @Query("limit") int limit, @Query("extended") Extended extended);
}
