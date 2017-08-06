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

import android.support.v4.util.LongSparseArray;
import android.text.format.DateUtils;
import net.simonvt.cathode.R;

public enum NotificationTime {
  MINUTES_15(R.string.preference_notification_time_minutes_15, 15 * DateUtils.MINUTE_IN_MILLIS),
  MINUTES_30(R.string.preference_notification_time_minutes_30, 30 * DateUtils.MINUTE_IN_MILLIS),
  HOURS_1(R.string.preference_notification_time_hours_1, DateUtils.HOUR_IN_MILLIS),
  HOURS_2(R.string.preference_notification_time_hours_2, 2 * DateUtils.HOUR_IN_MILLIS);

  private int stringRes;
  private long notificationTime;

  NotificationTime(int stringRes, long notificationTime) {
    this.stringRes = stringRes;
    this.notificationTime = notificationTime;
  }

  public int getStringRes() {
    return stringRes;
  }

  public long getNotificationTime() {
    return notificationTime;
  }

  private static final LongSparseArray<NotificationTime> TIME_MAPPING = new LongSparseArray<>();

  static {
    for (NotificationTime time : NotificationTime.values()) {
      TIME_MAPPING.put(time.notificationTime, time);
    }
  }

  public static NotificationTime fromValue(long cacheTime) {
    return TIME_MAPPING.get(cacheTime);
  }
}
