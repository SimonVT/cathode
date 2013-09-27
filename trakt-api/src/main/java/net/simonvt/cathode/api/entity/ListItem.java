package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.ListItemType;

public class ListItem {

  ListItemType type;

  Movie movie;

  TvShow show;

  String season;

  String episodeNum;

  Episode episode;

  public ListItemType getType() {
    return type;
  }

  public Movie getMovie() {
    return movie;
  }

  public TvShow getShow() {
    return show;
  }

  public String getSeason() {
    return season;
  }

  public String getEpisodeNum() {
    return episodeNum;
  }

  public Episode getEpisode() {
    return episode;
  }
}
