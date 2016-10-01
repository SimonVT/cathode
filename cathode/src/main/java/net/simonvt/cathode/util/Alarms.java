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

package net.simonvt.cathode.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

public final class Alarms {

  private Alarms() {
  }

  public static void setExactAndAllowWhileIdle(Context context, int type, long triggerAtMillis,
      PendingIntent pi) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      am.setExactAndAllowWhileIdle(type, triggerAtMillis, pi);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      am.setExact(type, triggerAtMillis, pi);
    } else {
      am.set(type, triggerAtMillis, pi);
    }
  }
}
