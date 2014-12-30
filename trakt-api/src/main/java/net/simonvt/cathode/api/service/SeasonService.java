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
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Rating;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.enumeration.Extended;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public interface SeasonService {

  /**
   * Returns all seasons for a show including the number of episodes in each season.
   *
   * @param id Show trakt ID
   */
  @GET("/shows/{id}/seasons") List<Season> getSummary(@Path("id") long id,
      @Query("extended") Extended extended);

  /**
   * Returns all episodes for a specific season of a show.
   *
   * @param id Show trakt ID
   */
  @GET("/shows/{id}/seasons/{season}") List<Episode> getSeason(@Path("id") long id,
      @Path("season") int season);

  @GET("/shows/{id[/seasons/{season[/ratings") Rating getRatings(@Path("id") long id,
      @Path("season") int season);
}
