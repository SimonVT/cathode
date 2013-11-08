package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum ActivityAction {
  ALL("all"),
  WATCHING("watching"),
  SCROBBLE("scrobble"),
  CHECKIN("checkin"),
  SEEN("seen"),
  COLLECTION("collection"),
  RATING("rating"),
  WATCHLIST("watchlist"),
  SHOUT("shout"),
  REVIEW("review"),
  CREATED("created"),
  ITEM_ADDED("item_added");

  private final String value;

  private ActivityAction(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  private static final Map<String, ActivityAction> STRING_MAPPING =
      new HashMap<String, ActivityAction>();

  static {
    for (ActivityAction via : ActivityAction.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static ActivityAction fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
