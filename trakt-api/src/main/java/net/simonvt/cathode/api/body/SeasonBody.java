package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;

public class SeasonBody {

  @SerializedName("tvdb_id") private Integer tvdbId;

  private String title;

  private Integer year;

  private Integer season;

  public SeasonBody(Integer tvdbId, Integer season) {
    this.tvdbId = tvdbId;
    this.season = season;
  }

  public SeasonBody(String title, Integer year, Integer season) {
    this.title = title;
    this.year = year;
    this.season = season;
  }
}
