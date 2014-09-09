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

public class ShowProgress {

  public static class Season {

    Integer number;

    Integer aired;

    Integer completed;

    List<Episode> episodes;

    public Integer getNumber() {
      return number;
    }

    public Integer getAired() {
      return aired;
    }

    public Integer getCompleted() {
      return completed;
    }

    public List<Episode> getEpisodes() {
      return episodes;
    }
  }

  public static class Episode {

    Integer number;

    Boolean completed;

    public Integer getNumber() {
      return number;
    }

    public Boolean getCompleted() {
      return completed;
    }
  }

  public static class NextEpisode {

    Integer season;

    Integer number;

    String title;

    Ids ids;

    public Integer getSeason() {
      return season;
    }

    public Integer getNumber() {
      return number;
    }

    public String getTitle() {
      return title;
    }

    public Ids getIds() {
      return ids;
    }
  }

  Integer aired;

  Integer completed;

  List<Season> seasons;

  public Integer getAired() {
    return aired;
  }

  public Integer getCompleted() {
    return completed;
  }

  public List<Season> getSeasons() {
    return seasons;
  }
}
