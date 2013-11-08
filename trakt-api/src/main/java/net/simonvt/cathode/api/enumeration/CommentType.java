package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum CommentType {
  ALL("all"),
  SHOUTS("shouts"),
  REVIEWS("reviews");

  private final String value;

  private CommentType(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  private static final Map<String, CommentType> STRING_MAPPING = new HashMap<String, CommentType>();

  static {
    for (CommentType via : CommentType.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static CommentType fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
