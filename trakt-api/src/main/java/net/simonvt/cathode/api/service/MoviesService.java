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
import net.simonvt.cathode.api.entity.AnticipatedItem;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.People;
import net.simonvt.cathode.api.entity.TrendingItem;
import net.simonvt.cathode.api.entity.UpdatedItem;
import net.simonvt.cathode.api.enumeration.Extended;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MoviesService {

  @GET("/movies/trending") Call<List<TrendingItem>> getTrendingMovies();

  @GET("/movies/trending") Call<List<TrendingItem>> getTrendingMovies(@Query("limit") int limit);

  @GET("/movies/trending") Call<List<TrendingItem>> getTrendingMovies(@Query("limit") int limit,
      @Query("extended") Extended extended);

  /**
   * Returns the most anticipated movies based on the number of lists a movie appears on.
   */
  @GET("/movies/anticipated") Call<List<AnticipatedItem>> getAnticipatedMovies();

  /**
   * Returns the most anticipated movies based on the number of lists a movie appears on.
   */
  @GET("/movies/anticipated") Call<List<AnticipatedItem>> getAnticipatedMovies(
      @Query("limit") int limit);

  /**
   * Returns the most anticipated movies based on the number of lists a movie appears on.
   */
  @GET("/movies/anticipated") Call<List<AnticipatedItem>> getAnticipatedMovies(
      @Query("limit") int limit, @Query("extended") Extended extended);

  @GET("/movies/updates/{start_date}") Call<List<UpdatedItem>> updated(
      @Path("start_date") String startDate, @Query("page") int page, @Query("limit") int limit);

  @GET("/movies/{id}") Call<Movie> getSummary(@Path("id") long traktId,
      @Query("extended") Extended extended);

  @GET("/movies/{id}/people") Call<People> getPeople(@Path("id") long traktId,
      @Query("extended") Extended extended);

  /**
   * <b>Pagination</b>
   * <p>
   * Returns all top level comments for a movie. Most recent comments returned first.
   */
  @GET("/movies/{id}/comments") Call<List<Comment>> getComments(@Path("id") long id,
      @Query("page") int page, @Query("limit") int limit, @Query("extended") Extended extended);

  /**
   * <b>Pagination</b>
   * <p>
   * Returns related and similar movies.
   */
  @GET("/movies/{id}/related") Call<List<Movie>> getRelated(@Path("id") long id,
      @Query("limit") int limit, @Query("extended") Extended extended);
}
