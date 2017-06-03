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

package net.simonvt.cathode.api.entity;

import java.util.List;

public class SyncResponse {

  public static class Success {

    private Integer movies;

    private Integer shows;

    private Integer seasons;

    private Integer episodes;

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
  }

  public static class Errors {

    private List<Movie> movies;

    private List<Show> shows;

    private List<Season> seasons;

    private List<Episode> episodes;

    private List<Integer> ids;

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

    public List<Integer> getIds() {
      return ids;
    }
  }

  private Success added;

  private Success existing;

  private Success deleted;

  private Errors notFound;

  public Success getAdded() {
    return added;
  }

  public Success getExisting() {
    return existing;
  }

  public Success getDeleted() {
    return deleted;
  }

  public Errors getNotFound() {
    return notFound;
  }
}
