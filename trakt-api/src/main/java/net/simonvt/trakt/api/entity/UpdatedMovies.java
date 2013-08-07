package net.simonvt.trakt.api.entity;

import java.util.List;

public class UpdatedMovies {

  public static class MovieTimestamp {

    private Long lastUpdated;

    private String imdbId;

    private Integer tmdbId;

    public Long getLastUpdated() {
      return lastUpdated;
    }

    public String getImdbId() {
      return imdbId;
    }

    public Integer getTmdbId() {
      return tmdbId;
    }
  }

  private Timestamp timestamps;

  private List<MovieTimestamp> movies;

  public Timestamp getTimestamps() {
    return timestamps;
  }

  public List<MovieTimestamp> getMovies() {
    return movies;
  }
}
