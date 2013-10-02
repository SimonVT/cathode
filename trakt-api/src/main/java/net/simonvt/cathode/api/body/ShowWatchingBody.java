package net.simonvt.cathode.api.body;

import com.google.gson.annotations.SerializedName;

public class ShowWatchingBody {

  @SerializedName("tvdb_id") private Integer tvdbId;

  private String title;

  private Integer year;

  private Integer season;

  private Integer episode;

  private Integer duration;

  private Integer progress;

  public static ShowWatchingBody tvdbId(int tvdbId, int season, int episode) {
    ShowWatchingBody body = new ShowWatchingBody();
    body.tvdbId = tvdbId;
    body.season = season;
    body.episode = episode;
    return body;
  }

  public static ShowWatchingBody title(String title, int year, int season, int episode) {
    ShowWatchingBody body = new ShowWatchingBody();
    body.title = title;
    body.year = year;
    body.season = season;
    body.episode = episode;
    return body;
  }

  public ShowWatchingBody duration(int duration) {
    this.duration = duration;
    return this;
  }

  public ShowWatchingBody progress(int progress) {
    this.progress = progress;
    return this;
  }
}
