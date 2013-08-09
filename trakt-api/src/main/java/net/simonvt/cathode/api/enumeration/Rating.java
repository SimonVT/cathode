package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum Rating {
  LOVE("love"),
  HATE("hate"),
  FALSE("false");

  private final String mValue;

  private Rating(String value) {
    mValue = value;
  }

  @Override
  public String toString() {
    return mValue;
  }

  private static final Map<String, Rating> STRING_MAPPING = new HashMap<String, Rating>();

  static {
    for (Rating via : Rating.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static Rating fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
