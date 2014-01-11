package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;

public class CheckinBody {

  private class Share {
    Boolean facebook;

    Boolean twitter;

    Boolean tumblr;

    Boolean path;
  }

  @SerializedName("tvdb_id") Integer tvdbId;

  @SerializedName("tmdb_id") Long tmdbId;

  String title;

  Integer year;

  Integer season;

  Integer episode;

  @SerializedName("episode_tvdb_id") Integer episodeTvdbId;

  Integer duration;

  @SerializedName("venue_id") Integer venueId;

  @SerializedName("venue_name") String venueName;

  Share share;

  String message;

  private CheckinBody() {
  }

  public static CheckinBody tvdbId(int tvdbId) {
    CheckinBody cb = new CheckinBody();
    cb.tvdbId = tvdbId;
    return cb;
  }

  public static CheckinBody tmdbId(long tmdbId) {
    CheckinBody cb = new CheckinBody();
    cb.tmdbId = tmdbId;
    return cb;
  }

  public static CheckinBody title(String title, int year) {
    CheckinBody cb = new CheckinBody();
    cb.title = title;
    cb.year = year;
    return cb;
  }

  public CheckinBody season(Integer season) {
    this.season = season;
    return this;
  }

  public CheckinBody episode(Integer episode) {
    this.episode = episode;
    return this;
  }

  public CheckinBody episodeTvdbId(Integer episodeTvdbId) {
    this.episodeTvdbId = episodeTvdbId;
    return this;
  }

  public CheckinBody duration(Integer duration) {
    this.duration = duration;
    return this;
  }

  public CheckinBody venueId(Integer venueId) {
    this.venueId = venueId;
    return this;
  }

  public CheckinBody venueName(String venueName) {
    this.venueName = venueName;
    return this;
  }

  public CheckinBody facebook(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.facebook = share;
    return this;
  }

  public CheckinBody twitter(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.twitter = share;
    return this;
  }

  public CheckinBody tumblr(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.tumblr = share;
    return this;
  }

  public CheckinBody path(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.path = share;
    return this;
  }

  public CheckinBody message(String message) {
    this.message = message;
    return this;
  }
}
