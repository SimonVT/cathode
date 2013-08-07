package net.simonvt.trakt.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum ShowStatus {
  CONTINUING("Continuing"),
  ENDED("Ended");

  private String mStatus;

  ShowStatus(String status) {
    mStatus = status;
  }

  @Override
  public String toString() {
    return mStatus;
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
