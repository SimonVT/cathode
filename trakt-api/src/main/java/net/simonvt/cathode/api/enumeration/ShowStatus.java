package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum ShowStatus {
  CONTINUING("Continuing"),
  ENDED("Ended");

  private String status;

  ShowStatus(String status) {
    this.status = status;
  }

  @Override public String toString() {
    return status;
  }

  private static final Map<String, ShowStatus> STRING_MAPPING = new HashMap<String, ShowStatus>();

  static {
    for (ShowStatus via : ShowStatus.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static ShowStatus fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
