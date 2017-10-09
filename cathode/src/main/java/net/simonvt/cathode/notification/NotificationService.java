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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import net.simonvt.cathode.common.util.WakeLock;

public class NotificationService extends IntentService {

  static final String LOCK_TAG = "NotificationService";

  public static void start(Context context) {
    Intent i = new Intent(context, NotificationService.class);
    context.startService(i);
  }

  public NotificationService() {
    super("ScheduleNotificationService");
  }

  @Override public void onCreate() {
    super.onCreate();
    WakeLock.acquire(this, LOCK_TAG);
  }

  @Override public void onDestroy() {
    WakeLock.release(this, LOCK_TAG);
    super.onDestroy();
  }

  @Override protected void onHandleIntent(Intent intent) {
    NotificationHelper.displayNotifications(this);
    NotificationHelper.scheduleNextNotification(this);
  }
}
