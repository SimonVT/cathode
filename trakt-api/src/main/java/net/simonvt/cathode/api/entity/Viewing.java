package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.RatingMode;

public class Viewing {

  public static class Ratings {

    private RatingMode mode;

    public RatingMode getMode() {
      return mode;
    }
  }

  public static class Shouts {

    private boolean showBadges;

    private boolean showSpoilers;

    public boolean getShowBadges() {
      return showBadges;
    }

    public boolean getShowSpoilers() {
      return showSpoilers;
    }
  }

  private Ratings ratings;

  private Shouts shouts;

  public Ratings getRatings() {
    return ratings;
  }

  public Shouts getShouts() {
    return shouts;
  }
}
