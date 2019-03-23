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

import net.simonvt.cathode.api.entity.Episode
import net.simonvt.cathode.api.entity.Rating
import net.simonvt.cathode.api.entity.Season
import net.simonvt.cathode.api.enumeration.Extended
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SeasonService {

  /**
   * Returns all seasons for a show including the number of episodes in each season.
   *
   * @param id Show trakt ID
   */
  @GET("/shows/{id}/seasons")
  fun getSummary(
    @Path("id") id: Long,
    @Query("extended") extended: Extended? = null
  ): Call<List<Season>>

  /**
   * Returns all episodes for a specific season of a show.
   *
   * @param id Show trakt ID
   */
  @GET("/shows/{id}/seasons/{season}")
  fun getSeason(
    @Path("id") id: Long,
    @Path("season") season: Int,
    @Query("extended") extended: Extended? = null
  ): Call<List<Episode>>

  @GET("/shows/{id[/seasons/{season[/ratings")
  fun getRatings(
    @Path("id") id: Long,
    @Path("season") season: Int
  ): Call<Rating>
}
