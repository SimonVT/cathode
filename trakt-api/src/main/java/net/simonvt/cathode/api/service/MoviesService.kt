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

import net.simonvt.cathode.api.entity.AnticipatedItem
import net.simonvt.cathode.api.entity.Comment
import net.simonvt.cathode.api.entity.Movie
import net.simonvt.cathode.api.entity.People
import net.simonvt.cathode.api.entity.TrendingItem
import net.simonvt.cathode.api.entity.UpdatedItem
import net.simonvt.cathode.api.enumeration.Extended
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MoviesService {

  @GET("/movies/trending")
  fun getTrendingMovies(
    @Query("limit") limit: Int = LIMIT,
    @Query("extended") extended: Extended? = null
  ): Call<List<TrendingItem>>

  /**
   * Returns the most anticipated movies based on the number of lists a movie appears on.
   */
  @GET("/movies/anticipated")
  fun getAnticipatedMovies(
    @Query("limit") limit: Int = LIMIT,
    @Query("extended") extended: Extended? = null
  ): Call<List<AnticipatedItem>>

  @GET("/movies/updates/{start_date}")
  fun updated(
    @Path("start_date") startDate: String,
    @Query("page") page: Int,
    @Query("limit") limit: Int = LIMIT
  ): Call<List<UpdatedItem>>

  @GET("/movies/{id}")
  fun getSummary(
    @Path("id") traktId: Long,
    @Query("extended") extended: Extended
  ): Call<Movie>

  @GET("/movies/{id}/people")
  fun getPeople(
    @Path("id") traktId: Long,
    @Query("extended") extended: Extended? = null
  ): Call<People>

  /**
   * **Pagination**
   *
   *
   * Returns all top level comments for a movie. Most recent comments returned first.
   */
  @GET("/movies/{id}/comments")
  fun getComments(
    @Path("id") id: Long,
    @Query("page") page: Int,
    @Query("limit") limit: Int = LIMIT,
    @Query("extended") extended: Extended? = null
  ): Call<List<Comment>>

  /**
   * **Pagination**
   *
   *
   * Returns related and similar movies.
   */
  @GET("/movies/{id}/related")
  fun getRelated(
    @Path("id") id: Long,
    @Query("limit") limit: Int = LIMIT,
    @Query("extended") extended: Extended? = null
  ): Call<List<Movie>>

  companion object {
    const val LIMIT = 100
  }
}
