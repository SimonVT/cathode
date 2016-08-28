/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.settings;

import android.text.format.DateUtils;
import java.util.HashMap;
import java.util.Map;
import net.simonvt.cathode.R;

public enum UpcomingTime {
  WEEKS_1(R.string.preference_upcoming_time_weeks_1, 1 * DateUtils.WEEK_IN_MILLIS),
  WEEKS_2(R.string.preference_upcoming_time_weeks_2, 2 * DateUtils.WEEK_IN_MILLIS),
  MONTHS_1(R.string.preference_upcoming_time_months_1, 31 * DateUtils.DAY_IN_MILLIS),
  END_OF_TIME(R.string.preference_upcoming_time_end, 0L);

  private int stringRes;
  private long cacheTime;

  UpcomingTime(int stringRes, long cacheTime) {
    this.stringRes = stringRes;
    this.cacheTime = cacheTime;
  }

  public int getStringRes() {
    return stringRes;
  }

  public long getCacheTime() {
    return cacheTime;
  }

  private static final Map<Long, UpcomingTime> TIME_MAPPING = new HashMap<>();

  static {
    for (UpcomingTime time : UpcomingTime.values()) {
      TIME_MAPPING.put(time.cacheTime, time);
    }
  }

  public static UpcomingTime fromValue(long cacheTime) {
    return TIME_MAPPING.get(cacheTime);
  }
}
