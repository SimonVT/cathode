package net.simonvt.cathode.api.entity;

public class Episode {

  private Integer season;

  private Integer number;

  private Integer tvdbId;

  private String imdbId;

  private String title;

  private String overview;

  private Long firstAired;

  private String firstAiredIso;

  private Long firstAiredUtc;

  private String url;

  private String screen;

  private Images images;

  private Ratings ratings;

  private Boolean watched;

  private Integer plays;

  private Boolean inWatchlist;

  private Boolean inCollection;

  private String rating;

  private Integer ratingAdvanced;

  public Integer getSeason() {
    return season;
  }

  public Integer getNumber() {
    return number;
  }

  public Integer getTvdbId() {
    return tvdbId;
  }

  public String getImdbId() {
    return imdbId;
  }

  public String getTitle() {
    return title;
  }

  public String getOverview() {
    return overview;
  }

  public Long getFirstAired() {
    return firstAired;
  }

  public String getFirstAiredIso() {
    return firstAiredIso;
  }

  public Long getFirstAiredUtc() {
    return firstAiredUtc;
  }

  public String getUrl() {
    return url;
  }

  public String getScreen() {
    return screen;
  }

  public Images getImages() {
    return images;
  }

  public Ratings getRatings() {
    return ratings;
  }

  public Boolean isWatched() {
    return watched;
  }

  public Integer getPlays() {
    return plays;
  }

  public Boolean isInWatchlist() {
    return inWatchlist;
  }

  public Boolean isInCollection() {
    return inCollection;
  }

  public String getRating() {
    return rating;
  }

  public Integer getRatingAdvanced() {
    return ratingAdvanced;
  }
}
