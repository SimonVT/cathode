package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum ActivityType {
  ALL("all"),
  EPISODE("episode"),
  SHOW("show"),
  MOVIE("movie"),
  LIST("list");

  private final String value;

  private ActivityType(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  private static final Map<String, ActivityType> STRING_MAPPING =
      new HashMap<String, ActivityType>();

  static {
    for (ActivityType via : ActivityType.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static ActivityType fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
