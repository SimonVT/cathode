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

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import dagger.android.AndroidInjection
import net.simonvt.cathode.CathodeApp
import net.simonvt.cathode.R
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.common.util.Longs
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.sync.trakt.CheckIn
import net.simonvt.cathode.ui.EpisodeDetailsActivity
import timber.log.Timber
import javax.inject.Inject

class NotificationActionService : JobIntentService() {

  @Inject
  lateinit var jobManager: JobManager
  @Inject
  lateinit var episodeHelper: EpisodeDatabaseHelper
  @Inject
  lateinit var checkIn: CheckIn

  override fun onCreate() {
    super.onCreate()
    AndroidInjection.inject(this)
  }

  override fun onHandleWork(intent: Intent) {
    val action = intent.action
    Timber.d("Action: %s", action)

    var notificationId = intent.getIntExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, -1)
    val id = intent.getLongExtra(NotificationActionReceiver.EXTRA_ID, -1L)

    when (action) {
      NotificationActionReceiver.ACTION_CHECK_IN -> {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationId > -1) {
          var notificationCount = -1
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationCount =
              NotificationHelper.getGroupNotificationCount(nm, NotificationHelper.GROUP)
          }

          nm.cancel(notificationId)

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (notificationCount <= 1) {
              nm.cancel(NotificationHelper.GROUP_NOTIFICATION_ID)
            }
          }
        }

        if (!checkIn.episode(id, null, false, false, false)) {
          notificationId = Longs.hashCode(id)

          val episode = contentResolver.query(
            Episodes.withId(id),
            arrayOf(
              EpisodeColumns.TITLE,
              EpisodeColumns.SHOW_ID,
              EpisodeColumns.SHOW_TITLE,
              EpisodeColumns.SEASON,
              EpisodeColumns.EPISODE,
              EpisodeColumns.WATCHED
            )
          )
          episode.moveToFirst()
          val showId = episode.getLong(EpisodeColumns.SHOW_ID)
          val showTitle = episode.getString(EpisodeColumns.SHOW_TITLE)
          val number = episode.getInt(EpisodeColumns.EPISODE)
          val season = episode.getInt(EpisodeColumns.SEASON)
          val watched = episode.getBoolean(EpisodeColumns.WATCHED)
          val episodeTitle =
            DataHelper.getEpisodeTitle(this, episode, season, number, watched, true)
          episode.close()

          Timber.d("Title: %s", episodeTitle)

          val notification =
            NotificationCompat.Builder(this, CathodeApp.CHANNEL_ERRORS).setShowWhen(false)
              .setContentTitle(getString(R.string.checkin_error_notification_title))
              .setContentText(getString(R.string.checkin_error_notification_body, episodeTitle))
              .setTicker(getString(R.string.checkin_error_notification_title))
              .setSmallIcon(R.drawable.ic_notification)
              .setAutoCancel(false)
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setCategory(NotificationCompat.CATEGORY_ERROR)
              .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
              .setLocalOnly(true)

          // Check-in action
          val checkInIntent = Intent(this, NotificationActionReceiver::class.java)
          checkInIntent.action = NotificationActionReceiver.ACTION_CHECK_IN
          checkInIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
          checkInIntent.putExtra(NotificationActionReceiver.EXTRA_ID, id)
          checkInIntent.data = Episodes.withId(id)
          val checkInPI = PendingIntent.getBroadcast(this, 0, checkInIntent, 0)
          val checkIn = getString(R.string.action_checkin)
          notification.addAction(0, checkIn, checkInPI)

          // Content intent
          val contentIntent = Intent(this, EpisodeDetailsActivity::class.java)
          contentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
          contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_ID, id)
          contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_SHOW_ID, showId)
          contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_SHOW_TITLE, showTitle)
          contentIntent.data = Episodes.withId(id)
          val contentPI =
            PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
          notification.setContentIntent(contentPI)

          nm.notify(notificationId, notification.build())
        }

        val values = ContentValues()
        values.put(EpisodeColumns.NOTIFICATION_DISMISSED, true)
        contentResolver.update(Episodes.withId(id), values, null, null)
      }

      NotificationActionReceiver.ACTION_DISMISS -> {
        val values = ContentValues()
        values.put(EpisodeColumns.NOTIFICATION_DISMISSED, true)
        contentResolver.update(Episodes.withId(id), values, null, null)
      }
    }
  }

  companion object {

    const val JOB_ID = 50

    internal fun enqueueWork(context: Context, work: Intent) {
      JobIntentService.enqueueWork(context, NotificationActionService::class.java, JOB_ID, work)
    }
  }
}
