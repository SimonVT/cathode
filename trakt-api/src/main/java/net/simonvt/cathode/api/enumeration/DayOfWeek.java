package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum DayOfWeek {

  SUNDAY("Sunday"),
  MONDAY("Monday"),
  TUESDAY("Tuesday"),
  WEDNESDAY("Wednesday"),
  THURSDAY("Thursday"),
  FRIDAY("Friday"),
  SATURDAY("Saturday");

  private final String value;

  private DayOfWeek(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  private static final Map<String, DayOfWeek> STRING_MAPPING = new HashMap<String, DayOfWeek>();

  static {
    for (DayOfWeek via : DayOfWeek.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static DayOfWeek fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }
}
