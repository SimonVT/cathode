package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;

public class RecommendationsBody {

  String genre;

  @SerializedName("start_year") Integer startYear;

  @SerializedName("end_year") Integer endYear;

  @SerializedName("hide_collected") Boolean hideCollected;

  @SerializedName("hide_watchlisted") Boolean hideWatchlisted;

  public RecommendationsBody genre(String genre) {
    this.genre = genre;
    return this;
  }

  public RecommendationsBody startYear(int startYear) {
    this.startYear = startYear;
    return this;
  }

  public RecommendationsBody endYear(int endYear) {
    this.endYear = endYear;
    return this;
  }

  public RecommendationsBody hideCollected(boolean hideCollected) {
    this.hideCollected = hideCollected;
    return this;
  }

  public RecommendationsBody hideWatchlisted(boolean hideWatchlisted) {
    this.hideWatchlisted = hideWatchlisted;
    return this;
  }
}
