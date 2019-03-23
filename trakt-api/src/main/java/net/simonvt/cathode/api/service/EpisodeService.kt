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

import net.simonvt.cathode.api.entity.Comment
import net.simonvt.cathode.api.entity.Episode
import net.simonvt.cathode.api.enumeration.Extended
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EpisodeService {

  /**
   * Returns a single episode's details.
   *
   * @param showId Trakt ID.
   * @param season Season number Example: 1.
   * @param episode Episode number Example: 1.
   */
  @GET("/shows/{id}/seasons/{season}/episodes/{episode}")
  fun getSummary(
    @Path("id") showId: Long,
    @Path("season") season: Int,
    @Path("episode") episode: Int,
    @Query("extended") extended: Extended? = null
  ): Call<Episode>

  /**
   * **Pagination**
   *
   *
   * Returns all top level comments for a movie. Most recent comments returned first.
   */
  @GET("/shows/{id}/seasons/{season}/episodes/{episode}/comments")
  fun getComments(
    @Path("id") showId: Long,
    @Path("season") season: Int,
    @Path("episode") episode: Int,
    @Query("page") page: Int,
    @Query("limit") limit: Int = LIMIT,
    @Query("extended") extended: Extended? = null
  ): Call<List<Comment>>

  companion object {
    const val LIMIT = 100
  }
}
