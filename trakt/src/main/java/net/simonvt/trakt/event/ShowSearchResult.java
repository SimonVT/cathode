package net.simonvt.trakt.event;

import java.util.List;

public class ShowSearchResult {

  private List<Long> showIds;

  public ShowSearchResult(List<Long> showIds) {
    this.showIds = showIds;
  }

  public List<Long> getShowIds() {
    return showIds;
  }
}
