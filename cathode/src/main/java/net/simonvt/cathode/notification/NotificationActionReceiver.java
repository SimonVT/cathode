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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import net.simonvt.cathode.common.util.WakeLock;

public class NotificationActionReceiver extends BroadcastReceiver {

  public static final String EXTRA_NOTIFICATION_ID = "notificationId";
  public static final String EXTRA_ID = "id";

  public static final String ACTION_CHECK_IN = "checkIn";
  public static final String ACTION_DISMISS = "dismiss";

  @Override public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();
    if (action == null) {
      return;
    }

    switch (action) {
      case ACTION_CHECK_IN:
      case ACTION_DISMISS: {
        WakeLock.acquire(context, NotificationActionService.LOCK_TAG);
        final int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        final long id = intent.getLongExtra(EXTRA_ID, -1L);

        Intent actionIntent = new Intent(context, NotificationActionService.class);
        actionIntent.setAction(action);
        actionIntent.putExtra(EXTRA_ID, id);
        actionIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        context.startService(actionIntent);
      }
    }
  }
}
