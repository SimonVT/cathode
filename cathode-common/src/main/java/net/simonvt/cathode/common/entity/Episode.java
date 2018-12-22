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

public class Episode {

  private long id;
  private Long showId;
  private Long seasonId;
  private Integer season;
  private Integer episode;
  private Integer numberAbs;
  private String title;
  private String overview;
  private Long traktId;
  private String imdbId;
  private Integer tvdbId;
  private Integer tmdbId;
  private Long tvrageId;
  private Long firstAired;
  private Long updatedAt;
  private Integer userRating;
  private Long ratedAt;
  private Float rating;
  private Integer votes;
  private Integer plays;
  private Boolean watched;
  private Long watchedAt;
  private Boolean inCollection;
  private Long collectedAt;
  private Boolean inWatchlist;
  private Long watchedlistedAt;
  private Boolean watching;
  private Boolean checkedIn;
  private Long checkinStartedAt;
  private Long checkinExpiresAt;
  private Long lastCommentSync;
  private Boolean notificationDismissed;
  private String showTitle;

  public Episode(long id, Long showId, Long seasonId, Integer season, Integer episode,
      Integer numberAbs, String title, String overview, Long traktId, String imdbId, Integer tvdbId,
      Integer tmdbId, Long tvrageId, Long firstAired, Long updatedAt, Integer userRating,
      Long ratedAt, Float rating, Integer votes, Integer plays, Boolean watched, Long watchedAt,
      Boolean inCollection, Long collectedAt, Boolean inWatchlist, Long watchedlistedAt,
      Boolean watching, Boolean checkedIn, Long checkinStartedAt, Long checkinExpiresAt,
      Long lastCommentSync, Boolean notificationDismissed, String showTitle) {
    this.id = id;
    this.showId = showId;
    this.seasonId = seasonId;
    this.season = season;
    this.episode = episode;
    this.numberAbs = numberAbs;
    this.title = title;
    this.overview = overview;
    this.traktId = traktId;
    this.imdbId = imdbId;
    this.tvdbId = tvdbId;
    this.tmdbId = tmdbId;
    this.tvrageId = tvrageId;
    this.firstAired = firstAired;
    this.updatedAt = updatedAt;
    this.userRating = userRating;
    this.ratedAt = ratedAt;
    this.rating = rating;
    this.votes = votes;
    this.plays = plays;
    this.watched = watched;
    this.watchedAt = watchedAt;
    this.inCollection = inCollection;
    this.collectedAt = collectedAt;
    this.inWatchlist = inWatchlist;
    this.watchedlistedAt = watchedlistedAt;
    this.watching = watching;
    this.checkedIn = checkedIn;
    this.checkinStartedAt = checkinStartedAt;
    this.checkinExpiresAt = checkinExpiresAt;
    this.lastCommentSync = lastCommentSync;
    this.notificationDismissed = notificationDismissed;
    this.showTitle = showTitle;
  }

  public long getId() {
    return id;
  }

  public Long getShowId() {
    return showId;
  }

  public Long getSeasonId() {
    return seasonId;
  }

  public Integer getSeason() {
    return season;
  }

  public Integer getEpisode() {
    return episode;
  }

  public Integer getNumberAbs() {
    return numberAbs;
  }

  public String getTitle() {
    return title;
  }

  public String getOverview() {
    return overview;
  }

  public Long getTraktId() {
    return traktId;
  }

  public String getImdbId() {
    return imdbId;
  }

  public Integer getTvdbId() {
    return tvdbId;
  }

  public Integer getTmdbId() {
    return tmdbId;
  }

  public Long getTvrageId() {
    return tvrageId;
  }

  public Long getFirstAired() {
    return firstAired;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public Integer getUserRating() {
    return userRating;
  }

  public Long getRatedAt() {
    return ratedAt;
  }

  public Float getRating() {
    return rating;
  }

  public Integer getVotes() {
    return votes;
  }

  public Integer getPlays() {
    return plays;
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

  public Long getWatchedlistedAt() {
    return watchedlistedAt;
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

  public Long getLastCommentSync() {
    return lastCommentSync;
  }

  public Boolean getNotificationDismissed() {
    return notificationDismissed;
  }

  public String getShowTitle() {
    return showTitle;
  }
}
