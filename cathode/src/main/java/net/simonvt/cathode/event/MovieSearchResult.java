package net.simonvt.cathode.event;

import java.util.List;

public class MovieSearchResult {

  private List<Long> movieIds;

  public MovieSearchResult(List<Long> movieIds) {
    this.movieIds = movieIds;
  }

  public List<Long> getMovieIds() {
    return movieIds;
  }
}
