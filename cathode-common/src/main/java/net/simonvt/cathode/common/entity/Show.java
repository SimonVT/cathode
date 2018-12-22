package net.simonvt.cathode.common.entity;

import net.simonvt.cathode.api.enumeration.ShowStatus;

public class Show {

  long id;
  String title;
  String titleNoArticle;
  Integer year;
  Long firstAired;
  String country;
  String overview;
  Integer runtime;
  String network;
  String airDay;
  String airTime;
  String airTimezone;
  String certification;
  String slug;
  Long traktId;
  String imdbId;
  Integer tvdbId;
  Integer tmdbId;
  Long tvrageId;
  Long lastUpdated;
  String trailer;
  String homepage;
  ShowStatus status;
  Integer userRating;
  Long ratedAt;
  Float rating;
  Integer votes;
  Integer watchers;
  Integer plays;
  Integer scrobbles;
  Integer checkins;
  Boolean inWatchlist;
  Long watchlistedAt;
  Long lastWatchedAt;
  Long lastCollectedAt;
  Boolean hiddenCalendar;
  Boolean hiddenWatched;
  Boolean hiddenCollected;
  Boolean hiddenRecommendations;
  Integer watchedCount;
  Integer airdateCount;
  Integer inCollectionCount;
  Integer inWatchlistCount;
  Boolean needsSync;
  Long lastSync;
  Long lastCommentSync;
  Long lastCreditsSync;
  Long lastRelatedSync;
  Boolean watching;
  Integer airedCount;
  Integer unairedCount;
  Integer episodeCount;
  Long watchingEpisodeId;

  public Show(long id, String title, String titleNoArticle, Integer year, Long firstAired,
      String country, String overview, Integer runtime, String network, String airDay,
      String airTime, String airTimezone, String certification, String slug, Long traktId,
      String imdbId, Integer tvdbId, Integer tmdbId, Long tvrageId, Long lastUpdated,
      String trailer, String homepage, ShowStatus status, Integer userRating, Long ratedAt,
      Float rating, Integer votes, Integer watchers, Integer plays, Integer scrobbles,
      Integer checkins, Boolean inWatchlist, Long watchlistedAt, Long lastWatchedAt,
      Long lastCollectedAt, Boolean hiddenCalendar, Boolean hiddenWatched, Boolean hiddenCollected,
      Boolean hiddenRecommendations, Integer watchedCount, Integer airdateCount,
      Integer inCollectionCount, Integer inWatchlistCount, Boolean needsSync, Long lastSync,
      Long lastCommentSync, Long lastCreditsSync, Long lastRelatedSync, Boolean watching,
      Integer airedCount, Integer unairedCount, Integer episodeCount, Long watchingEpisodeId) {
    this.id = id;
    this.title = title;
    this.titleNoArticle = titleNoArticle;
    this.year = year;
    this.firstAired = firstAired;
    this.country = country;
    this.overview = overview;
    this.runtime = runtime;
    this.network = network;
    this.airDay = airDay;
    this.airTime = airTime;
    this.airTimezone = airTimezone;
    this.certification = certification;
    this.slug = slug;
    this.traktId = traktId;
    this.imdbId = imdbId;
    this.tvdbId = tvdbId;
    this.tmdbId = tmdbId;
    this.tvrageId = tvrageId;
    this.lastUpdated = lastUpdated;
    this.trailer = trailer;
    this.homepage = homepage;
    this.status = status;
    this.userRating = userRating;
    this.ratedAt = ratedAt;
    this.rating = rating;
    this.votes = votes;
    this.watchers = watchers;
    this.plays = plays;
    this.scrobbles = scrobbles;
    this.checkins = checkins;
    this.inWatchlist = inWatchlist;
    this.watchlistedAt = watchlistedAt;
    this.lastWatchedAt = lastWatchedAt;
    this.lastCollectedAt = lastCollectedAt;
    this.hiddenCalendar = hiddenCalendar;
    this.hiddenWatched = hiddenWatched;
    this.hiddenCollected = hiddenCollected;
    this.hiddenRecommendations = hiddenRecommendations;
    this.watchedCount = watchedCount;
    this.airdateCount = airdateCount;
    this.inCollectionCount = inCollectionCount;
    this.inWatchlistCount = inWatchlistCount;
    this.needsSync = needsSync;
    this.lastSync = lastSync;
    this.lastCommentSync = lastCommentSync;
    this.lastCreditsSync = lastCreditsSync;
    this.lastRelatedSync = lastRelatedSync;
    this.watching = watching;
    this.airedCount = airedCount;
    this.unairedCount = unairedCount;
    this.episodeCount = episodeCount;
    this.watchingEpisodeId = watchingEpisodeId;
  }

  public long getId() {
    return id;
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

  public Long getFirstAired() {
    return firstAired;
  }

  public String getCountry() {
    return country;
  }

  public String getOverview() {
    return overview;
  }

  public Integer getRuntime() {
    return runtime;
  }

  public String getNetwork() {
    return network;
  }

  public String getAirDay() {
    return airDay;
  }

  public String getAirTime() {
    return airTime;
  }

  public String getAirTimezone() {
    return airTimezone;
  }

  public String getCertification() {
    return certification;
  }

  public String getSlug() {
    return slug;
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

  public Long getLastUpdated() {
    return lastUpdated;
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

  public Integer getWatchers() {
    return watchers;
  }

  public Integer getPlays() {
    return plays;
  }

  public Integer getScrobbles() {
    return scrobbles;
  }

  public Integer getCheckins() {
    return checkins;
  }

  public Boolean getInWatchlist() {
    return inWatchlist;
  }

  public Long getWatchlistedAt() {
    return watchlistedAt;
  }

  public Long getLastWatchedAt() {
    return lastWatchedAt;
  }

  public Long getLastCollectedAt() {
    return lastCollectedAt;
  }

  public Boolean getHiddenCalendar() {
    return hiddenCalendar;
  }

  public Boolean getHiddenWatched() {
    return hiddenWatched;
  }

  public Boolean getHiddenCollected() {
    return hiddenCollected;
  }

  public Boolean getHiddenRecommendations() {
    return hiddenRecommendations;
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

  public Boolean getWatching() {
    return watching;
  }

  public Integer getAiredCount() {
    return airedCount;
  }

  public Integer getUnairedCount() {
    return unairedCount;
  }

  public Integer getEpisodeCount() {
    return episodeCount;
  }

  public Long getWatchingEpisodeId() {
    return watchingEpisodeId;
  }
}
