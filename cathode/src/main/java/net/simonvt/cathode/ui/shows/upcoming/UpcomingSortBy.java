/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.shows.upcoming;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public enum UpcomingSortBy {
  TITLE("title", Shows.SORT_TITLE),
  NEXT_EPISODE("nextEpisode", Shows.SORT_NEXT_EPISODE),
  LAST_WATCHED("lastWatched", Shows.SORT_LAST_WATCHED);

  private String key;

  private String sortOrder;

  UpcomingSortBy(String key, String sortOrder) {
    this.key = key;
    this.sortOrder = sortOrder;
  }

  public String getKey() {
    return key;
  }

  public String getSortOrder() {
    return sortOrder;
  }

  @Override public String toString() {
    return key;
  }

  private static final Map<String, UpcomingSortBy> STRING_MAPPING = new HashMap<>();

  static {
    for (UpcomingSortBy via : UpcomingSortBy.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
    }
  }

  public static UpcomingSortBy fromValue(String value) {
    UpcomingSortBy sortBy = STRING_MAPPING.get(value.toUpperCase(Locale.US));
    if (sortBy == null) {
      sortBy = TITLE;
    }
    return sortBy;
  }
}
