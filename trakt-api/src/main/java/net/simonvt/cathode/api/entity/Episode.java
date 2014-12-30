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

public class Episode {

  Integer season;

  Integer number;

  String title;

  Ids ids;

  Images images;

  Integer numberAbs;

  String overview;

  IsoTime firstAired;

  IsoTime updatedAt;

  Float rating;

  Integer votes;

  List<String> availableTranslations;

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

  public Images getImages() {
    return images;
  }

  public Integer getNumberAbs() {
    return numberAbs;
  }

  public String getOverview() {
    return overview;
  }

  public IsoTime getFirstAired() {
    return firstAired;
  }

  public IsoTime getUpdatedAt() {
    return updatedAt;
  }

  public Float getRating() {
    return rating;
  }

  public Integer getVotes() {
    return votes;
  }

  public List<String> getAvailableTranslations() {
    return availableTranslations;
  }
}
