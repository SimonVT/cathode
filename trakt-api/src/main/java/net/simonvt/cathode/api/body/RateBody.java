package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RateBody {

  private static class Show {

    @SerializedName("tvdb_id") Integer tvdbId;

    Integer rating;

    private Show(Integer tvdbId, Integer rating) {
      this.tvdbId = tvdbId;
      this.rating = rating;
    }
  }

  private static class Episode extends Show {

    Integer season;

    Integer episode;

    private Episode(Integer tvdbId, Integer rating, Integer season, Integer episode) {
      super(tvdbId, rating);
      this.season = season;
      this.episode = episode;
    }
  }

  private static class Movie {

    @SerializedName("tmdb_id") Long tmdbId;

    Integer rating;

    private Movie(Long tmdbId, Integer rating) {
      this.tmdbId = tmdbId;
      this.rating = rating;
    }
  }

  @SerializedName("tvdb_id") private Integer tvdbId;

  @SerializedName("tmdb_id") private Long tmdbId;

  private Integer season;

  private Integer episode;

  private Integer rating;

  private List<Show> shows;

  private List<Episode> episodes;

  private List<Movie> movies;

  public RateBody show(int tvdbId, int rating) {
    this.tvdbId = tvdbId;
    this.rating = rating;
    return this;
  }

  public RateBody episode(int tvdbId, int season, int episode, int rating) {
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
    this.rating = rating;
    return this;
  }

  public RateBody movie(long tmdbId, int rating) {
    this.tmdbId = tmdbId;
    this.rating = rating;
    return this;
  }

  public RateBody shows(int tvdbId, int rating) {
    shows.add(new Show(tvdbId, rating));
    return this;
  }

  public RateBody episodes(int tvdbId, int season, int episode, int rating) {
    episodes.add(new Episode(tvdbId, season, episode, rating));
    return this;
  }

  public RateBody movies(long tmdbId, int rating) {
    movies.add(new Movie(tmdbId, rating));
    return this;
  }
}
