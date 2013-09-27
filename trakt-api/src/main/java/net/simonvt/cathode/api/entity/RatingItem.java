package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.Rating;

public class RatingItem {

  private Long inserted;

  private Rating rating;

  private Integer ratingAdvanced;

  private TvShow show;

  private Episode episode;

  private String title;

  private Integer year;

  private String imdbId;

  private String tmdbId;

  private Integer tvdbId;

  private Integer tvrageId;

  public Long getInserted() {
    return inserted;
  }

  public Rating getRating() {
    return rating;
  }

  public Integer getRatingAdvanced() {
    return ratingAdvanced;
  }

  public TvShow getShow() {
    return show;
  }

  public Episode getEpisode() {
    return episode;
  }

  public String getTitle() {
    return title;
  }

  public Integer getYear() {
    return year;
  }

  public String getImdbId() {
    return imdbId;
  }

  public String getTmdbId() {
    return tmdbId;
  }

  public Integer getTvdbId() {
    return tvdbId;
  }

  public Integer getTvrageId() {
    return tvrageId;
  }
}
