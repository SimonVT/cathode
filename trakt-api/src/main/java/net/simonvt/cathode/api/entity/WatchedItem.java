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

public class WatchedItem {

  public static class Season {

    Integer number;

    List<Episode> episodes;

    public Integer getNumber() {
      return number;
    }

    public List<Episode> getEpisodes() {
      return episodes;
    }
  }

  public static class Episode {

    Integer number;

    Integer plays;

    public Integer getNumber() {
      return number;
    }

    public Integer getPlays() {
      return plays;
    }
  }

  Integer plays;

  IsoTime lastWatchedAt;

  Show show;

  List<Season> seasons;

  Movie movie;

  public Integer getPlays() {
    return plays;
  }

  public IsoTime getLastWatchedAt() {
    return lastWatchedAt;
  }

  public Show getShow() {
    return show;
  }

  public List<Season> getSeasons() {
    return seasons;
  }

  public Movie getMovie() {
    return movie;
  }
}
