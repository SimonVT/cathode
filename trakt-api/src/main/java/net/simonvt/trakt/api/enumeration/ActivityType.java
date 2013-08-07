package net.simonvt.trakt.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum ActivityType {
  ALL("all"),
  EPISODE("episode"),
  SHOW("show"),
  MOVIE("movie"),
  LIST("list");

  private final String mValue;

  private ActivityType(String value) {
    mValue = value;
  }

  @Override
  public String toString() {
    return mValue;
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
