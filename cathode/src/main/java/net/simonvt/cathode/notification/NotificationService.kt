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

import android.app.IntentService
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.simonvt.cathode.CathodeApp
import net.simonvt.cathode.R
import net.simonvt.cathode.common.util.WakeLock

class NotificationService : IntentService("NotificationService") {

  override fun onCreate() {
    super.onCreate()
    WakeLock.acquire(this, LOCK_TAG)

    CathodeApp.createSyncChannel(this)
    val notification = NotificationCompat.Builder(this, CathodeApp.CHANNEL_SYNC)
      .setContentTitle(getText(R.string.notification_reminders_title))
      .setSmallIcon(R.drawable.ic_notification)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
    startForeground(NotificationHelper.GROUP_NOTIFICATION_ID - 1, notification)
  }

  override fun onDestroy() {
    WakeLock.release(this, LOCK_TAG)
    super.onDestroy()
  }

  override fun onHandleIntent(intent: Intent?) {
    NotificationHelper.displayNotifications(this)
    NotificationHelper.scheduleNextNotification(this)
  }

  companion object {

    const val LOCK_TAG = "NotificationService"

    @JvmStatic
    fun start(context: Context) {
      val i = Intent(context, NotificationService::class.java)
      ContextCompat.startForegroundService(context, i)
    }
  }
}
