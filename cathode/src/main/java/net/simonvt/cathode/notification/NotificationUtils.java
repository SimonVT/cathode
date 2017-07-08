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
package net.simonvt.cathode.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class NotificationUtils {

  private NotificationUtils() {
  }

  public static void scheduleNotifications(Context context) {
    Intent i = new Intent(context, NotificationService.class);
    context.startService(i);
  }

  public static void scheduleNotifications(Context context, long inMillis) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    Intent i = new Intent(context, NotificationReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + inMillis, pi);
  }
}
