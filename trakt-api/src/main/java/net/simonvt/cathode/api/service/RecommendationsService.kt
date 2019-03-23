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

import net.simonvt.cathode.api.entity.Movie
import net.simonvt.cathode.api.entity.Show
import net.simonvt.cathode.api.enumeration.Extended
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RecommendationsService {

  /**
   * **OAuth Required**
   *
   *
   * Personalized movie recommendations for a user. Results returned with the top recommendation
   * first.
   */
  @GET("/recommendations/movies")
  fun movies(
    @Query("limit") limit: Int = LIMIT,
    @Query("extended") extended: Extended? = null
  ): Call<List<Movie>>

  /**
   * **OAuth Required**
   *
   *
   * Dismiss a movie from getting recommended anymore.
   */
  @DELETE("/recommendations/movies/{id}")
  fun dismissMovie(@Path("id") id: Long): Call<ResponseBody>

  /**
   * **OAuth Required**
   *
   *
   * Personalized show recommendations for a user. Results returned with the top recommendation
   * first.
   */
  @GET("/recommendations/shows")
  fun shows(
    @Query("limit") limit: Int = LIMIT,
    @Query("extended") extended: Extended? = null
  ): Call<List<Show>>

  /**
   * **OAuth Required**
   *
   *
   * Dismiss a show from getting recommended anymore.
   */
  @DELETE("recommendations/shows/{id}")
  fun dismissShow(@Path("id") id: Long): Call<ResponseBody>

  companion object {
    const val LIMIT = 100
  }
}
