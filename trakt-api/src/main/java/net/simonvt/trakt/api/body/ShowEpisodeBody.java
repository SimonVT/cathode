package net.simonvt.trakt.api.body;

import com.google.gson.annotations.SerializedName;

public class ShowEpisodeBody {

  @SerializedName("tvdb_id") int tvdbId;

  Episode[] episodes;

  public ShowEpisodeBody(int tvdbId, int season, int episode) {
    this(tvdbId, new Episode(season, episode));
  }

  public ShowEpisodeBody(int tvdbId, Episode... episodes) {
    this.tvdbId = tvdbId;
    this.episodes = episodes;
  }

  public static class Episode {

    private int season;

    private int episode;

    public Episode(int season, int episode) {
      this.season = season;
      this.episode = episode;
    }
  }
}
