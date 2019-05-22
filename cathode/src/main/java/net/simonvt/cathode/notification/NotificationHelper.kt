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

package net.simonvt.cathode.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.simonvt.cathode.R
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.common.util.Alarms
import net.simonvt.cathode.common.util.Longs
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.provider.util.SqlColumn
import net.simonvt.cathode.settings.NotificationTime
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.ui.EpisodeDetailsActivity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object NotificationHelper {

  private val SORT_KEY_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

  const val GROUP = "upcoming"

  private const val CHANNEL_REMINDERS = "channel_reminders"
  const val GROUP_NOTIFICATION_ID = Integer.MAX_VALUE - 1

  private val VIBRATION = longArrayOf(0, 100, 200, 100, 200, 100)

  private const val EPISODE_ID = "episodeId"

  private val PROJECTION = arrayOf(
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.RUNTIME),
    SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.ID) + " AS " + EPISODE_ID,
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.TITLE),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.WATCHED),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED),
    SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.NOTIFICATION_DISMISSED)
  )

  private var timeDateFormat: DateFormat? = null

  fun displayNotifications(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val advanceMillis = Settings.get(context)
      .getLong(Settings.NOTIFICACTION_TIME, NotificationTime.HOURS_1.notificationTime)
    val vibrate = Settings.get(context).getBoolean(Settings.NOTIFICACTION_VIBRATE, true)
    val sound = Settings.get(context).getBoolean(Settings.NOTIFICACTION_SOUND, true)

    createReminderChannel(context)

    val currentTime = System.currentTimeMillis()
    val notifyAiredBefore = currentTime + advanceMillis

    val episodes = context.contentResolver.query(
      Episodes.EPISODES_WITH_SHOW,
      PROJECTION,
      "(" + SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT) + ">0 OR " +
          SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST) + "=1) AND " +
          SqlColumn.table(Tables.SHOWS).column(ShowColumns.HIDDEN_CALENDAR) + "=0 AND " +
          SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.WATCHED) + "=0 AND " +
          SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED) + ">? AND " +
          SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED) + "<?",
      arrayOf(
        (currentTime - 12 * DateUtils.HOUR_IN_MILLIS).toString(),
        notifyAiredBefore.toString()
      ),
      Shows.SORT_NEXT_EPISODE
    )!!

    while (episodes.moveToNext()) {
      val showTitle = episodes.getString(ShowColumns.TITLE)

      val episodeId = episodes.getLong("episodeId")
      val season = episodes.getInt(EpisodeColumns.SEASON)
      val episode = episodes.getInt(EpisodeColumns.EPISODE)
      val watched = episodes.getBoolean(EpisodeColumns.WATCHED)
      val episodeTitle = DataHelper.getEpisodeTitle(context, episodes, season, episode, watched)
      val firstAired = DataHelper.getFirstAired(episodes)
      val notificationDismissed = episodes.getBoolean(EpisodeColumns.NOTIFICATION_DISMISSED)

      if (notificationDismissed) {
        continue
      }

      if (firstAired < currentTime) {
        // Airing or aired
        displayNotification(
          context,
          showTitle,
          episodeId,
          episodeTitle,
          season,
          episode,
          watched,
          firstAired,
          true
        )
      } else {
        // Advance notification
        displayNotification(
          context,
          showTitle,
          episodeId,
          episodeTitle,
          season,
          episode,
          watched,
          firstAired,
          false
        )
      }
    }

    episodes.close()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      if (getGroupNotificationCount(nm, GROUP) == 0) {
        nm.cancel(GROUP_NOTIFICATION_ID)
      } else {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
          .setShowWhen(false)
          .setSmallIcon(R.drawable.ic_notification)
          .setGroup(GROUP)
          .setGroupSummary(true)
          .setPriority(NotificationCompat.PRIORITY_HIGH)
          .setCategory(NotificationCompat.CATEGORY_ALARM)
          .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
          .setLocalOnly(true)
          .setOnlyAlertOnce(true)

        if (vibrate) {
          summaryNotification.setVibrate(VIBRATION)
        }
        if (sound) {
          summaryNotification.setSound(soundUri)
        }

        nm.notify(GROUP_NOTIFICATION_ID, summaryNotification.build())
      }
    }
  }

  fun scheduleNextNotification(context: Context) {
    val currentTime = System.currentTimeMillis()
    val maxDelay = currentTime + TimeUnit.HOURS.toMillis(12)

    val episodes = context.contentResolver.query(
      Episodes.EPISODES,
      arrayOf(EpisodeColumns.ID, EpisodeColumns.FIRST_AIRED),
      EpisodeColumns.FIRST_AIRED + ">?",
      arrayOf(currentTime.toString()),
      EpisodeColumns.FIRST_AIRED + " ASC LIMIT 1"
    )
    if (episodes!!.moveToFirst()) {
      val firstAired = episodes.getLong(EpisodeColumns.FIRST_AIRED)

      if (firstAired < maxDelay) {
        scheduleExact(context, maxDelay)
      } else {
        schedule(context, maxDelay)
      }
    } else {
      schedule(context, maxDelay)
    }
    episodes.close()
  }

  fun schedule(context: Context, millis: Long) {
    val i = Intent(context, NotificationReceiver::class.java)
    val pi = PendingIntent.getService(context, 0, i, 0)
    Alarms.set(context, AlarmManager.RTC_WAKEUP, millis, pi)
  }

  fun scheduleExact(context: Context, millis: Long) {
    val i = Intent(context, NotificationReceiver::class.java)
    val pi = PendingIntent.getBroadcast(context, 0, i, 0)
    Alarms.setExactAndAllowWhileIdle(context, AlarmManager.RTC_WAKEUP, millis, pi)
  }

  @RequiresApi(Build.VERSION_CODES.N)
  internal fun getGroupNotificationCount(nm: NotificationManager, group: String): Int {
    val notifications = nm.activeNotifications
    var count = 0
    for (statusBarNotification in notifications) {
      val n = statusBarNotification.notification
      if (n.flags and Notification.FLAG_GROUP_SUMMARY != Notification.FLAG_GROUP_SUMMARY && group == n.group) {
        count++
      }
    }
    return count
  }

  private fun displayNotification(
    context: Context,
    showTitle: String,
    episodeId: Long,
    episodeTitle: String,
    season: Int,
    episode: Int,
    watched: Boolean,
    firstAired: Long,
    airing: Boolean
  ) {
    val epiTitle = DataHelper.getEpisodeTitle(context, episodeTitle, season, episode, watched, true)

    val contentTitle = "$showTitle - $epiTitle"

    val contentText: String
    val tickerText: String

    val time = getDateFormat(context).format(Date(firstAired))

    if (airing) {
      contentText = context.getString(R.string.notification_aired_at, time)
      tickerText = context.getString(R.string.notification_show_aired_at, showTitle, time)
    } else {
      contentText = context.getString(R.string.notification_airing_at, time)
      tickerText = context.getString(R.string.notification_show_airing_at, showTitle, time)
    }

    val notificationId = Longs.hashCode(episodeId)

    val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS) //
      .setShowWhen(false)
      .setContentTitle(contentTitle)
      .setContentText(contentText)
      .setTicker(tickerText)
      .setSmallIcon(R.drawable.ic_notification)
      .setSortKey(createSortKey(firstAired))
      .setAutoCancel(false)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setLocalOnly(true)
      .setGroup(GROUP)

    // Check-in action
    val checkInIntent = Intent(context, NotificationActionReceiver::class.java)
    checkInIntent.action = NotificationActionReceiver.ACTION_CHECK_IN
    checkInIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
    checkInIntent.putExtra(NotificationActionReceiver.EXTRA_ID, episodeId)
    checkInIntent.data = Episodes.withId(episodeId)
    val checkInPI = PendingIntent.getBroadcast(context, 0, checkInIntent, 0)
    val checkIn = context.getString(R.string.action_checkin)
    notification.addAction(0, checkIn, checkInPI)

    // Delete intent
    val dismissIntent = Intent(context, NotificationActionReceiver::class.java)
    dismissIntent.action = NotificationActionReceiver.ACTION_DISMISS
    dismissIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
    dismissIntent.putExtra(NotificationActionReceiver.EXTRA_ID, episodeId)
    dismissIntent.data = Episodes.withId(episodeId)
    val dismissPI = PendingIntent.getBroadcast(context, 0, dismissIntent, 0)
    notification.setDeleteIntent(dismissPI)

    // Content intent
    val contentIntent = Intent(context, EpisodeDetailsActivity::class.java)
    contentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_ID, episodeId)
    contentIntent.data = Episodes.withId(episodeId)
    val contentPI =
      PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    notification.setContentIntent(contentPI)

    val nm = NotificationManagerCompat.from(context)
    nm.notify(notificationId, notification.build())
  }

  private fun createSortKey(firstAired: Long): String {
    return SORT_KEY_FORMAT.format(firstAired)
  }

  private fun getDateFormat(context: Context): DateFormat {
    if (timeDateFormat == null) {
      timeDateFormat = if (android.text.format.DateFormat.is24HourFormat(context)) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
      } else {
        SimpleDateFormat("h:mm a", Locale.getDefault())
      }
      timeDateFormat!!.timeZone = TimeZone.getDefault()
    }

    return timeDateFormat!!
  }

  private fun createReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val name = context.getString(R.string.channel_reminders)
      val description = context.getString(R.string.channel_reminders_description)
      val channel =
        NotificationChannel(CHANNEL_REMINDERS, name, NotificationManager.IMPORTANCE_HIGH)
      channel.description = description
      channel.enableLights(true)
      channel.enableVibration(true)
      nm.createNotificationChannel(channel)
    }
  }
}
