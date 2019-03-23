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

public class Season {

  long id;
  Long showId;
  int season;
  Integer tvdbId;
  Integer tmdbId;
  Long tvrageId;
  Integer userRating;
  Long ratedAt;
  Float rating;
  Integer votes;
  Boolean hiddenWatched;
  Boolean hiddenCollected;
  Integer watchedCount;
  Integer airdateCount;
  Integer inCollectionCount;
  Integer inWatchlistCount;
  Boolean needsSync;
  String showTitle;
  Integer airedCount;
  Integer unairedCount;
  Integer watchedAiredCount;
  Integer collectedAiredCount;
  Integer episodeCount;

  public Season(long id, Long showId, int season, Integer tvdbId, Integer tmdbId, Long tvrageId,
      Integer userRating, Long ratedAt, Float rating, Integer votes, Boolean hiddenWatched,
      Boolean hiddenCollected, Integer watchedCount, Integer airdateCount,
      Integer inCollectionCount, Integer inWatchlistCount, Boolean needsSync, String showTitle,
      Integer airedCount, Integer unairedCount, Integer watchedAiredCount,
      Integer collectedAiredCount, Integer episodeCount) {
    this.id = id;
    this.showId = showId;
    this.season = season;
    this.tvdbId = tvdbId;
    this.tmdbId = tmdbId;
    this.tvrageId = tvrageId;
    this.userRating = userRating;
    this.ratedAt = ratedAt;
    this.rating = rating;
    this.votes = votes;
    this.hiddenWatched = hiddenWatched;
    this.hiddenCollected = hiddenCollected;
    this.watchedCount = watchedCount;
    this.airdateCount = airdateCount;
    this.inCollectionCount = inCollectionCount;
    this.inWatchlistCount = inWatchlistCount;
    this.needsSync = needsSync;
    this.showTitle = showTitle;
    this.airedCount = airedCount;
    this.unairedCount = unairedCount;
    this.watchedAiredCount = watchedAiredCount;
    this.collectedAiredCount = collectedAiredCount;
    this.episodeCount = episodeCount;
  }

  public long getId() {
    return id;
  }

  public Long getShowId() {
    return showId;
  }

  public int getSeason() {
    return season;
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

  public Boolean getHiddenWatched() {
    return hiddenWatched;
  }

  public Boolean getHiddenCollected() {
    return hiddenCollected;
  }

  public Integer getWatchedCount() {
    return watchedCount;
  }

  public Integer getAirdateCount() {
    return airdateCount;
  }

  public Integer getInCollectionCount() {
    return inCollectionCount;
  }

  public Integer getInWatchlistCount() {
    return inWatchlistCount;
  }

  public Boolean getNeedsSync() {
    return needsSync;
  }

  public String getShowTitle() {
    return showTitle;
  }

  public Integer getAiredCount() {
    return airedCount;
  }

  public Integer getUnairedCount() {
    return unairedCount;
  }

  public Integer getWatchedAiredCount() {
    return watchedAiredCount;
  }

  public Integer getCollectedAiredCount() {
    return collectedAiredCount;
  }

  public Integer getEpisodeCount() {
    return episodeCount;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Season season1 = (Season) o;

    if (id != season1.id) return false;
    if (season != season1.season) return false;
    if (showId != null ? !showId.equals(season1.showId) : season1.showId != null) return false;
    if (tvdbId != null ? !tvdbId.equals(season1.tvdbId) : season1.tvdbId != null) return false;
    if (tmdbId != null ? !tmdbId.equals(season1.tmdbId) : season1.tmdbId != null) return false;
    if (tvrageId != null ? !tvrageId.equals(season1.tvrageId) : season1.tvrageId != null) {
      return false;
    }
    if (userRating != null ? !userRating.equals(season1.userRating) : season1.userRating != null) {
      return false;
    }
    if (ratedAt != null ? !ratedAt.equals(season1.ratedAt) : season1.ratedAt != null) return false;
    if (rating != null ? !rating.equals(season1.rating) : season1.rating != null) return false;
    if (votes != null ? !votes.equals(season1.votes) : season1.votes != null) return false;
    if (hiddenWatched != null ? !hiddenWatched.equals(season1.hiddenWatched)
        : season1.hiddenWatched != null) {
      return false;
    }
    if (hiddenCollected != null ? !hiddenCollected.equals(season1.hiddenCollected)
        : season1.hiddenCollected != null) {
      return false;
    }
    if (watchedCount != null ? !watchedCount.equals(season1.watchedCount)
        : season1.watchedCount != null) {
      return false;
    }
    if (airdateCount != null ? !airdateCount.equals(season1.airdateCount)
        : season1.airdateCount != null) {
      return false;
    }
    if (inCollectionCount != null ? !inCollectionCount.equals(season1.inCollectionCount)
        : season1.inCollectionCount != null) {
      return false;
    }
    if (inWatchlistCount != null ? !inWatchlistCount.equals(season1.inWatchlistCount)
        : season1.inWatchlistCount != null) {
      return false;
    }
    if (needsSync != null ? !needsSync.equals(season1.needsSync) : season1.needsSync != null) {
      return false;
    }
    if (showTitle != null ? !showTitle.equals(season1.showTitle) : season1.showTitle != null) {
      return false;
    }
    if (airedCount != null ? !airedCount.equals(season1.airedCount) : season1.airedCount != null) {
      return false;
    }
    if (unairedCount != null ? !unairedCount.equals(season1.unairedCount)
        : season1.unairedCount != null) {
      return false;
    }
    if (watchedAiredCount != null ? !watchedAiredCount.equals(season1.watchedAiredCount)
        : season1.watchedAiredCount != null) {
      return false;
    }
    if (collectedAiredCount != null ? !collectedAiredCount.equals(season1.collectedAiredCount)
        : season1.collectedAiredCount != null) {
      return false;
    }
    return episodeCount != null ? episodeCount.equals(season1.episodeCount)
        : season1.episodeCount == null;
  }

  @Override public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (showId != null ? showId.hashCode() : 0);
    result = 31 * result + season;
    result = 31 * result + (tvdbId != null ? tvdbId.hashCode() : 0);
    result = 31 * result + (tmdbId != null ? tmdbId.hashCode() : 0);
    result = 31 * result + (tvrageId != null ? tvrageId.hashCode() : 0);
    result = 31 * result + (userRating != null ? userRating.hashCode() : 0);
    result = 31 * result + (ratedAt != null ? ratedAt.hashCode() : 0);
    result = 31 * result + (rating != null ? rating.hashCode() : 0);
    result = 31 * result + (votes != null ? votes.hashCode() : 0);
    result = 31 * result + (hiddenWatched != null ? hiddenWatched.hashCode() : 0);
    result = 31 * result + (hiddenCollected != null ? hiddenCollected.hashCode() : 0);
    result = 31 * result + (watchedCount != null ? watchedCount.hashCode() : 0);
    result = 31 * result + (airdateCount != null ? airdateCount.hashCode() : 0);
    result = 31 * result + (inCollectionCount != null ? inCollectionCount.hashCode() : 0);
    result = 31 * result + (inWatchlistCount != null ? inWatchlistCount.hashCode() : 0);
    result = 31 * result + (needsSync != null ? needsSync.hashCode() : 0);
    result = 31 * result + (showTitle != null ? showTitle.hashCode() : 0);
    result = 31 * result + (airedCount != null ? airedCount.hashCode() : 0);
    result = 31 * result + (unairedCount != null ? unairedCount.hashCode() : 0);
    result = 31 * result + (watchedAiredCount != null ? watchedAiredCount.hashCode() : 0);
    result = 31 * result + (collectedAiredCount != null ? collectedAiredCount.hashCode() : 0);
    result = 31 * result + (episodeCount != null ? episodeCount.hashCode() : 0);
    return result;
  }
}
