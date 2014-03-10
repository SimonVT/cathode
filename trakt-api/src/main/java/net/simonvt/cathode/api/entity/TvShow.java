package net.simonvt.cathode.api.entity;

import java.util.List;
import net.simonvt.cathode.api.enumeration.DayOfWeek;
import net.simonvt.cathode.api.enumeration.Rating;
import net.simonvt.cathode.api.enumeration.ShowStatus;

public class TvShow {

  public static class People {

    private List<Person> actors;

    public List<Person> getActors() {
      return actors;
    }
  }

  private String title;

  private Integer year;

  private String imdbId;

  private Integer tvdbId;

  private Integer tvrageId;

  private String url;

  private Images images;

  private List<String> genres;

  private Long firstAired;

  private String firstAiredIso;

  private Long firstAiredUtc;

  private String country;

  private String overview;

  private Integer runtime;

  private String network;

  private DayOfWeek airDay;

  private String airTime;

  private String certification;

  private ShowStatus status;

  private Rating rating;

  private Integer ratingAdvanced;

  private Ratings ratings;

  private Stats stats;

  private Long lastUpdated;

  private List<Season> seasons;

  private List<Episode> episodes;

  private Boolean inWatchlist;

  private People people;

  public String getTitle() {
    return title;
  }

  public Integer getYear() {
    return year;
  }

  public String getImdbId() {
    return imdbId;
  }

  public Integer getTvdbId() {
    return tvdbId;
  }

  public Integer getTvrageId() {
    return tvrageId;
  }

  public String getUrl() {
    return url;
  }

  public Images getImages() {
    return images;
  }

  public List<String> getGenres() {
    return genres;
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

  public DayOfWeek getAirDay() {
    return airDay;
  }

  public String getAirTime() {
    return airTime;
  }

  public String getCertification() {
    return certification;
  }

  public ShowStatus getStatus() {
    return status;
  }

  public Rating getRating() {
    return rating;
  }

  public Integer getRatingAdvanced() {
    return ratingAdvanced;
  }

  public Ratings getRatings() {
    return ratings;
  }

  public Stats getStats() {
    return stats;
  }

  public Long getLastUpdated() {
    return lastUpdated;
  }

  public List<Season> getSeasons() {
    return seasons;
  }

  public List<Episode> getEpisodes() {
    return episodes;
  }

  public Boolean isInWatchlist() {
    return inWatchlist;
  }

  public People getPeople() {
    return people;
  }
}
