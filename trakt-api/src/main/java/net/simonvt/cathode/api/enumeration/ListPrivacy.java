package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum ListPrivacy {
  PUBLIC("public"),
  FRIENDS("friends"),
  PRIVATE("private");

  private final String value;

  private ListPrivacy(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  private static final Map<String, ListPrivacy> STRING_MAPPING = new HashMap<String, ListPrivacy>();

  static {
    for (ListPrivacy via : ListPrivacy.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static ListPrivacy fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
