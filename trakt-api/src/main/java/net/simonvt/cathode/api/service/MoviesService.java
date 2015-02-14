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
import net.simonvt.cathode.api.entity.People;
import net.simonvt.cathode.api.entity.TrendingItem;
import net.simonvt.cathode.api.entity.UpdatedItem;
import net.simonvt.cathode.api.enumeration.Extended;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public interface MoviesService {

  @GET("/movies/trending") List<TrendingItem> getTrendingMovies();

  @GET("/movies/updates/{start_date}") List<UpdatedItem> updated(
      @Path("start_date") String startDate, @Query("page") int page, @Query("limit") int limit);

  @GET("/movies/{id}") Movie getSummary(@Path("id") long traktId,
      @Query("extended") Extended extended);

  @GET("/movies/{id}/people") People getPeople(@Path("id") long traktId);
}
