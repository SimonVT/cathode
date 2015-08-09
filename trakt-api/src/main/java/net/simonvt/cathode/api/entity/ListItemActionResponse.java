/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.entity;

import java.util.List;

public class ListItemActionResponse {

  public static class Items {

    private Integer movies;

    private Integer shows;

    private Integer seasons;

    private Integer episodes;

    private Integer people;

    public Integer getMovies() {
      return movies;
    }

    public Integer getShows() {
      return shows;
    }

    public Integer getSeasons() {
      return seasons;
    }

    public Integer getEpisodes() {
      return episodes;
    }

    public Integer getPeople() {
      return people;
    }
  }

  public static class NotFound {

    private List<Movie> movies;

    private List<Show> shows;

    private List<Season> seasons;

    private List<Episode> episodes;

    public List<Movie> getMovies() {
      return movies;
    }

    public List<Show> getShows() {
      return shows;
    }

    public List<Season> getSeasons() {
      return seasons;
    }

    public List<Episode> getEpisodes() {
      return episodes;
    }
  }

  private Items added;

  private Items deleted;

  private Items existing;

  private NotFound notFound;

  public Items getAdded() {
    return added;
  }

  public Items getDeleted() {
    return deleted;
  }

  public Items getExisting() {
    return existing;
  }

  public NotFound getNotFound() {
    return notFound;
  }
}
