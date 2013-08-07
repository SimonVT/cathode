package net.simonvt.trakt.api.entity;

import java.util.List;

public class Movie {

  public static class People {

    private List<Person> directors;

    private List<Person> writers;

    private List<Person> producers;

    private List<Person> actors;

    public List<Person> getDirectors() {
      return directors;
    }

    public List<Person> getWriters() {
      return writers;
    }

    public List<Person> getProducers() {
      return producers;
    }

    public List<Person> getActors() {
      return actors;
    }
  }

  private String title;

  private Integer year;

  private Long released;

  private String url;

  private String trailer;

  private Integer runtime;

  private String tagline;

  private String overview;

  private String certification;

  private String imdbId;

  private Long tmdbId;

  private Long rtId;

  private long lastUpdated;

  private String poster;

  private Images images;

  private List<UserProfile> topWatchers;

  private Ratings ratings;

  private Stats stats;

  private People people;

  private List<String> genres;

  private Boolean watched;

  private Boolean unseen;

  private Integer plays;

  private Boolean rating;

  private Integer ratingAdvanced;

  private Boolean inWatchlist;

  private Boolean inCollection;

  public String getTitle() {
    return title;
  }

  public Integer getYear() {
    return year;
  }

  public Long getReleased() {
    return released;
  }

  public String getUrl() {
    return url;
  }

  public String getTrailer() {
    return trailer;
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

  public String getImdbId() {
    return imdbId;
  }

  public Long getTmdbId() {
    return tmdbId;
  }

  public Long getRtId() {
    return rtId;
  }

  public long getLastUpdated() {
    return lastUpdated;
  }

  public String getPoster() {
    return poster;
  }

  public Images getImages() {
    return images;
  }

  public List<UserProfile> getTopWatchers() {
    return topWatchers;
  }

  public Ratings getRatings() {
    return ratings;
  }

  public Stats getStats() {
    return stats;
  }

  public People getPeople() {
    return people;
  }

  public List<String> getGenres() {
    return genres;
  }

  public Boolean isWatched() {
    if (watched != null) {
      return watched;
    } else if (unseen != null) {
      return !unseen;
    }

    return null;
  }

  public Integer getPlays() {
    return plays;
  }

  public Boolean getRating() {
    return rating;
  }

  public Integer getRatingAdvanced() {
    return ratingAdvanced;
  }

  public Boolean isInWatchlist() {
    return inWatchlist;
  }

  public Boolean isInCollection() {
    return inCollection;
  }
}
