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
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.Extended;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RecommendationsService {

  /**
   * <b>OAuth Required</b>
   * <p>
   * Personalized movie recommendations for a user. Results returned with the top recommendation
   * first.
   */
  @GET("/recommendations/movies") Call<List<Movie>> movies();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Personalized movie recommendations for a user. Results returned with the top recommendation
   * first.
   */
  @GET("/recommendations/movies") Call<List<Movie>> movies(@Query("limit") int limit);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Personalized movie recommendations for a user. Results returned with the top recommendation
   * first.
   */
  @GET("/recommendations/movies") Call<List<Movie>> movies(@Query("limit") int limit,
      @Query("extended") Extended extended);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Dismiss a movie from getting recommended anymore.
   */
  @DELETE("/recommendations/movies/{id}") Call<ResponseBody> dismissMovie(@Path("id") long id);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Personalized show recommendations for a user. Results returned with the top recommendation
   * first.
   */
  @GET("/recommendations/shows") Call<List<Show>> shows();

  /**
   * <b>OAuth Required</b>
   * <p>
   * Personalized show recommendations for a user. Results returned with the top recommendation
   * first.
   */
  @GET("/recommendations/shows") Call<List<Show>> shows(@Query("limit") int limit);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Personalized show recommendations for a user. Results returned with the top recommendation
   * first.
   */
  @GET("/recommendations/shows") Call<List<Show>> shows(@Query("limit") int limit,
      @Query("extended") Extended extended);

  /**
   * <b>OAuth Required</b>
   * <p>
   * Dismiss a show from getting recommended anymore.
   */
  @DELETE("recommendations/shows/{id}") Call<ResponseBody> dismissShow(@Path("id") long id);
}
