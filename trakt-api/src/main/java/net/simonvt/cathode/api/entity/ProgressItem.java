package net.simonvt.cathode.api.entity;

import java.util.List;
import java.util.Map;

public class ProgressItem {

  public class Progress {

    private Integer percentage;

    private Integer aired;

    private Integer completed;

    private Integer left;

    public Integer getPercentage() {
      return percentage;
    }

    public Integer getAired() {
      return aired;
    }

    public Integer getCompleted() {
      return completed;
    }

    public Integer getLeft() {
      return left;
    }
  }

  public class Season {

    private Integer season;

    private Integer percentage;

    private Integer aired;

    private Integer completed;

    private Integer left;

    private Map<Integer, Boolean> episodes;

    public Integer getSeason() {
      return season;
    }

    public Integer getPercentage() {
      return percentage;
    }

    public Integer getAired() {
      return aired;
    }

    public Integer getCompleted() {
      return completed;
    }

    public Integer getLeft() {
      return left;
    }

    public Map<Integer, Boolean> getEpisodes() {
      return episodes;
    }
  }

  private TvShow show;

  private Progress progress;

  private List<Integer> hiddenEpisodes;

  private Stats stats;

  private Episode nextEpisode;

  public TvShow getShow() {
    return show;
  }

  public Progress getProgress() {
    return progress;
  }

  public List<Integer> getHiddenEpisodes() {
    return hiddenEpisodes;
  }

  public Stats getStats() {
    return stats;
  }

  public Episode getNextEpisode() {
    return nextEpisode;
  }
}
