package net.simonvt.cathode.api.entity;

import java.util.List;

public class Season {

  public static class Episodes {

    private Integer count;

    private List<Integer> numbers;

    private List<Episode> episodes;

    public Integer getCount() {
      return count;
    }

    public List<Integer> getNumbers() {
      return numbers;
    }

    public List<Episode> getEpisodes() {
      return episodes;
    }
  }

  private Integer season;

  private Episodes episodes;

  private String url;

  private Images images;

  public Integer getSeason() {
    return season;
  }

  public Episodes getEpisodes() {
    return episodes;
  }

  public String getUrl() {
    return url;
  }

  public Images getImages() {
    return images;
  }
}
