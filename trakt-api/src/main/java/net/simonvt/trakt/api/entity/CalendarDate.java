package net.simonvt.trakt.api.entity;

import java.util.Date;
import java.util.List;

public class CalendarDate {

  public static class CalendarTvShowEpisode {

    private TvShow show;

    private Episode episode;

    public TvShow getShow() {
      return show;
    }

    public Episode getEpisode() {
      return episode;
    }
  }

  private Date date;

  private List<CalendarTvShowEpisode> episodes;

  public Date getDate() {
    return date;
  }

  public List<CalendarTvShowEpisode> getEpisodes() {
    return episodes;
  }
}
