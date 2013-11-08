package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;

public class RateBody {

  private static final String TAG = "RateBody";

  @SerializedName("tvdb_id") private Long tvdbId;

  @SerializedName("tmdb_id") private Long tmdbId;

  private int episode;

  private int rating;

  public static class Builder {

    RateBody body = new RateBody();

    public Builder tvdbId(Long tvdbId) {
      body.tvdbId = tvdbId;
      return this;
    }

    public Builder tmdbId(Long tmdbId) {
      body.tmdbId = tmdbId;
      return this;
    }

    public Builder episode(int episode) {
      body.episode = episode;
      return this;
    }

    public Builder rating(int rating) {
      body.rating = rating;
      return this;
    }

    public RateBody build() {
      return body;
    }
  }
}
