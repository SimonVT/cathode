package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;

public class DismissBody {

  @SerializedName("imdb_id") String imdbId;

  @SerializedName("tmdb_id") Long tmdbId;

  @SerializedName("tvdb_id") Integer tvdbId;

  String title;

  Integer year;

  public DismissBody imdbId(String imdbId) {
    this.imdbId = imdbId;
    return this;
  }

  public DismissBody tmdbId(long tmdbId) {
    this.tmdbId = tmdbId;
    return this;
  }

  public DismissBody tvdbId(int tvdbId) {
    this.tvdbId = tvdbId;
    return this;
  }

  public DismissBody title(String title) {
    this.title = title;
    return this;
  }

  public DismissBody year(int year) {
    this.year = year;
    return this;
  }
}
