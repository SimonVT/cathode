package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;

public class ScrobbleBody {

  private class Share {
    Boolean facebook;

    Boolean twitter;

    Boolean tumblr;

    Boolean path;
  }

  @SerializedName("tvdb_id") Integer tvdbId;

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

  Integer progress;

  private ScrobbleBody() {
  }

  public static ScrobbleBody tvdbId(int tvdbId) {
    ScrobbleBody cb = new ScrobbleBody();
    cb.tvdbId = tvdbId;
    return cb;
  }

  public static ScrobbleBody title(String title, int year) {
    ScrobbleBody cb = new ScrobbleBody();
    cb.title = title;
    cb.year = year;
    return cb;
  }

  public ScrobbleBody season(Integer season) {
    this.season = season;
    return this;
  }

  public ScrobbleBody episode(Integer episode) {
    this.episode = episode;
    return this;
  }

  public ScrobbleBody episodeTvdbId(Integer episodeTvdbId) {
    this.episodeTvdbId = episodeTvdbId;
    return this;
  }

  public ScrobbleBody duration(Integer duration) {
    this.duration = duration;
    return this;
  }

  public ScrobbleBody venueId(Integer venueId) {
    this.venueId = venueId;
    return this;
  }

  public ScrobbleBody venueName(String venueName) {
    this.venueName = venueName;
    return this;
  }

  public ScrobbleBody facebook(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.facebook = share;
    return this;
  }

  public ScrobbleBody twitter(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.twitter = share;
    return this;
  }

  public ScrobbleBody tumblr(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.tumblr = share;
    return this;
  }

  public ScrobbleBody path(boolean share) {
    if (this.share == null) this.share = new Share();
    this.share.path = share;
    return this;
  }

  public ScrobbleBody message(String message) {
    this.message = message;
    return this;
  }

  public ScrobbleBody progress(Integer progress) {
    this.progress = progress;
    return this;
  }
}
