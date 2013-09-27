package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum Sort {
  TITLE("title"),
  ACTIVITY("activity"),
  MOST_COMPLETED("most-completed"),
  LEAST_COMPLETED("least-completed"),
  RECENTLY_AIRED("recently-aired"),
  PREVIOUSLY_AIRED("previously-aired");

  private String value;

  Sort(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  private static final Map<String, Sort> STRING_MAPPING = new HashMap<String, Sort>();

  static {
    for (Sort via : Sort.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static Sort fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
