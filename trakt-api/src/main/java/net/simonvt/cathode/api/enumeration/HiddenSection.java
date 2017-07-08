/*
 * Copyright (C) 2015 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum HiddenSection {
  CALENDAR("calendar"),
  PROGRESS_WATCHED("progress_watched"),
  PROGRESS_COLLECTED("progress_collected"),
  RECOMMENDATIONS("recommendations");

  private final String value;

  HiddenSection(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  private static final Map<String, HiddenSection> STRING_MAPPING = new HashMap<>();

  static {
    for (HiddenSection via : HiddenSection.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
    }
  }

  public static HiddenSection fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase(Locale.US));
  }
}
