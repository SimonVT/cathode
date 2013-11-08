package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum Rating {
  LOVE("love"),
  HATE("hate"),
  FALSE("false");

  private final String value;

  private Rating(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
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
