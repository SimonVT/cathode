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
package net.simonvt.cathode.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_CHECK_IN, ACTION_DISMISS -> {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val id = intent.getLongExtra(EXTRA_ID, -1L)

        val actionIntent = Intent(intent.action)
        actionIntent.putExtra(EXTRA_ID, id)
        actionIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        NotificationActionService.enqueueWork(context, actionIntent)
      }
    }
  }

  companion object {

    const val EXTRA_NOTIFICATION_ID = "notificationId"
    const val EXTRA_ID = "id"

    const val ACTION_CHECK_IN = "checkIn"
    const val ACTION_DISMISS = "dismiss"
  }
}
