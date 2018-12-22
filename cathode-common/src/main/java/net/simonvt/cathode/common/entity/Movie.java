/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.entity;

public class Movie {

  long id;
  Long traktId;
  String imdbId;
  Integer tmdbId;
  String title;
  String titleNoArticle;
  Integer year;
  String released;
  Integer runtime;
  String tagline;
  String overview;
  String certification;
  String language;
  String homepage;
  String trailer;
  Integer userRating;
  Float rating;
  Integer votes;
  Boolean watched;
  Long watchedAt;
  Boolean inCollection;
  Long collectedAt;
  Boolean inWatchlist;
  Long watchlistedAt;
  Boolean watching;
  Boolean checkedIn;
  Long checkinStartedAt;
  Long checkinExpiresAt;
  Boolean needsSync;
  Long lastSync;
  Long lastCommentSync;
  Long lastCreditsSync;
  Long lastRelatedSync;

  public Movie(long id, Long traktId, String imdbId, Integer tmdbId, String title,
      String titleNoArticle, Integer year, String released, Integer runtime, String tagline,
      String overview, String certification, String language, String homepage, String trailer,
      Integer userRating, Float rating, Integer votes, Boolean watched, Long watchedAt,
      Boolean inCollection, Long collectedAt, Boolean inWatchlist, Long watchlistedAt,
      Boolean watching, Boolean checkedIn, Long checkinStartedAt, Long checkinExpiresAt,
      Boolean needsSync, Long lastSync, Long lastCommentSync, Long lastCreditsSync,
      Long lastRelatedSync) {
    this.id = id;
    this.traktId = traktId;
    this.imdbId = imdbId;
    this.tmdbId = tmdbId;
    this.title = title;
    this.titleNoArticle = titleNoArticle;
    this.year = year;
    this.released = released;
    this.runtime = runtime;
    this.tagline = tagline;
    this.overview = overview;
    this.certification = certification;
    this.language = language;
    this.homepage = homepage;
    this.trailer = trailer;
    this.userRating = userRating;
    this.rating = rating;
    this.votes = votes;
    this.watched = watched;
    this.watchedAt = watchedAt;
    this.inCollection = inCollection;
    this.collectedAt = collectedAt;
    this.inWatchlist = inWatchlist;
    this.watchlistedAt = watchlistedAt;
    this.watching = watching;
    this.checkedIn = checkedIn;
    this.checkinStartedAt = checkinStartedAt;
    this.checkinExpiresAt = checkinExpiresAt;
    this.needsSync = needsSync;
    this.lastSync = lastSync;
    this.lastCommentSync = lastCommentSync;
    this.lastCreditsSync = lastCreditsSync;
    this.lastRelatedSync = lastRelatedSync;
  }

  public long getId() {
    return id;
  }

  public Long getTraktId() {
    return traktId;
  }

  public String getImdbId() {
    return imdbId;
  }

  public Integer getTmdbId() {
    return tmdbId;
  }

  public String getTitle() {
    return title;
  }

  public String getTitleNoArticle() {
    return titleNoArticle;
  }

  public Integer getYear() {
    return year;
  }

  public String getReleased() {
    return released;
  }

  public Integer getRuntime() {
    return runtime;
  }

  public String getTagline() {
    return tagline;
  }

  public String getOverview() {
    return overview;
  }

  public String getCertification() {
    return certification;
  }

  public String getLanguage() {
    return language;
  }

  public String getHomepage() {
    return homepage;
  }

  public String getTrailer() {
    return trailer;
  }

  public Integer getUserRating() {
    return userRating;
  }

  public Float getRating() {
    return rating;
  }

  public Integer getVotes() {
    return votes;
  }

  public Boolean getWatched() {
    return watched;
  }

  public Long getWatchedAt() {
    return watchedAt;
  }

  public Boolean getInCollection() {
    return inCollection;
  }

  public Long getCollectedAt() {
    return collectedAt;
  }

  public Boolean getInWatchlist() {
    return inWatchlist;
  }

  public Long getWatchlistedAt() {
    return watchlistedAt;
  }

  public Boolean getWatching() {
    return watching;
  }

  public Boolean getCheckedIn() {
    return checkedIn;
  }

  public Long getCheckinStartedAt() {
    return checkinStartedAt;
  }

  public Long getCheckinExpiresAt() {
    return checkinExpiresAt;
  }

  public Boolean getNeedsSync() {
    return needsSync;
  }

  public Long getLastSync() {
    return lastSync;
  }

  public Long getLastCommentSync() {
    return lastCommentSync;
  }

  public Long getLastCreditsSync() {
    return lastCreditsSync;
  }

  public Long getLastRelatedSync() {
    return lastRelatedSync;
  }
}
