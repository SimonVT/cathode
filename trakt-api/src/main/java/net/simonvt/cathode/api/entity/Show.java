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
import net.simonvt.cathode.api.enumeration.ShowStatus;

public class Show {

  String title;

  Integer year;

  Ids ids;

  String overview;

  String firstAired;

  Airs airs;

  Integer runtime;

  String certification;

  String network;

  String country;

  IsoTime updatedAt;

  String trailer;

  String homepage;

  ShowStatus status;

  Float rating;

  String language;

  List<String> availableTranslations;

  List<String> genres;

  Images images;

  public String getTitle() {
    return title;
  }

  public Integer getYear() {
    return year;
  }

  public Ids getIds() {
    return ids;
  }

  public String getOverview() {
    return overview;
  }

  public String getFirstAired() {
    return firstAired;
  }

  public Airs getAirs() {
    return airs;
  }

  public Integer getRuntime() {
    return runtime;
  }

  public String getCertification() {
    return certification;
  }

  public String getNetwork() {
    return network;
  }

  public String getCountry() {
    return country;
  }

  public IsoTime getUpdatedAt() {
    return updatedAt;
  }

  public String getTrailer() {
    return trailer;
  }

  public String getHomepage() {
    return homepage;
  }

  public ShowStatus getStatus() {
    return status;
  }

  public Float getRating() {
    return rating;
  }

  public String getLanguage() {
    return language;
  }

  public List<String> getAvailableTranslations() {
    return availableTranslations;
  }

  public List<String> getGenres() {
    return genres;
  }

  public Images getImages() {
    return images;
  }
}
