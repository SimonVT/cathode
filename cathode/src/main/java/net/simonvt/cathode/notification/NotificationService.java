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
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.format.DateUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.util.Alarms;
import net.simonvt.cathode.common.util.Longs;
import net.simonvt.cathode.common.util.WakeLock;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.settings.NotificationTime;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.EpisodeDetailsActivity;
import net.simonvt.cathode.util.DataHelper;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.schematic.Cursors;

public class NotificationService extends IntentService {

  static final String LOCK_TAG = "NotificationService";

  private static final DateFormat SORT_KEY_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

  static final String GROUP = "upcoming";

  static final int GROUP_NOTIFICATION_ID = Integer.MAX_VALUE - 1;

  private static final long[] VIBRATION = new long[] {
      0, 100, 200, 100, 200, 100
  };

  private static final String[] PROJECTION = new String[] {
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.RUNTIME),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.ID) + " AS episodeId",
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.TITLE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.WATCHED),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.NOTIFICATION_DISMISSED),
  };

  private DateFormat timeDateFormat;

  private NotificationManager nm;

  public NotificationService() {
    super("ScheduleNotificationService");
  }

  @Override public void onCreate() {
    super.onCreate();
    WakeLock.acquire(this, LOCK_TAG);

    if (android.text.format.DateFormat.is24HourFormat(this)) {
      timeDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    } else {
      timeDateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    timeDateFormat.setTimeZone(TimeZone.getDefault());

    nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
  }

  @Override public void onDestroy() {
    WakeLock.release(this, LOCK_TAG);
    super.onDestroy();
  }

  @Override protected void onHandleIntent(Intent intent) {
    final long currentTime = System.currentTimeMillis();

    long nextNotificationTime = Long.MAX_VALUE;

    final boolean enabled = Settings.get(this).getBoolean(Settings.NOTIFICACTIONS_ENABLED, false);

    if (!enabled) {
      nm.cancel(GROUP_NOTIFICATION_ID);
      return;
    }

    final long advanceMillis = Settings.get(this)
        .getLong(Settings.NOTIFICACTION_TIME, NotificationTime.HOURS_1.getNotificationTime());
    final boolean vibrate = Settings.get(this).getBoolean(Settings.NOTIFICACTION_VIBRATE, true);
    final boolean sound = Settings.get(this).getBoolean(Settings.NOTIFICACTION_SOUND, true);

    Cursor episodes = getContentResolver().query(Episodes.EPISODES_WITH_SHOW, PROJECTION,
        "(" + SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT) + ">0 OR " + SqlColumn
            .table(Tables.SHOWS)
            .column(ShowColumns.IN_WATCHLIST) + "=1) AND " + SqlColumn.table(Tables.SHOWS)
            .column(ShowColumns.HIDDEN_CALENDAR) + "=0 AND (" + SqlColumn.table(Tables.EPISODES)
            .column(EpisodeColumns.FIRST_AIRED) + ">?)", new String[] {
            String.valueOf(currentTime - 6 * DateUtils.HOUR_IN_MILLIS)
        }, Shows.SORT_NEXT_EPISODE);

    while (episodes.moveToNext()) {
      final long showId = Cursors.getLong(episodes, ShowColumns.ID);
      final String showTitle = Cursors.getString(episodes, ShowColumns.TITLE);
      final int runtime = Cursors.getInt(episodes, ShowColumns.RUNTIME);

      final long episodeId = Cursors.getLong(episodes, "episodeId");
      final int season = Cursors.getInt(episodes, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(episodes, EpisodeColumns.EPISODE);
      final boolean watched = Cursors.getBoolean(episodes, EpisodeColumns.WATCHED);
      final String episodeTitle =
          DataHelper.getEpisodeTitle(this, episodes, season, episode, watched);
      final long firstAired = DataHelper.getFirstAired(episodes);
      final boolean notificationDismissed =
          Cursors.getBoolean(episodes, EpisodeColumns.NOTIFICATION_DISMISSED);

      final long airingEnd = firstAired + runtime * DateUtils.MINUTE_IN_MILLIS;
      final long advanceTime = firstAired - advanceMillis;

      if (notificationDismissed) {
        continue;
      }

      if (advanceTime > currentTime) {
        if (nextNotificationTime > advanceTime) {
          nextNotificationTime = advanceTime;
        }
      } else if (advanceTime <= currentTime && firstAired > currentTime) {
        // Advance notification
        displayNotification(showId, showTitle, episodeId, episodeTitle, season, episode, watched,
            firstAired, false);

        if (nextNotificationTime > firstAired) {
          nextNotificationTime = firstAired;
        }
      } else if (airingEnd > currentTime) {
        // Airing
        displayNotification(showId, showTitle, episodeId, episodeTitle, season, episode, watched,
            firstAired, true);

        if (nextNotificationTime > airingEnd) {
          nextNotificationTime = airingEnd;
        }
      } else {
        nm.cancel(Longs.hashCode(episodeId));
      }
    }

    episodes.close();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      if (getGroupNotificationCount(nm, GROUP) == 0) {
        nm.cancel(GROUP_NOTIFICATION_ID);
      } else {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder summaryNotification =
            new NotificationCompat.Builder(this).setShowWhen(false)
                .setSmallIcon(R.drawable.ic_notification)
                .setGroup(GROUP)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true)
                .setOnlyAlertOnce(true);

        if (vibrate) {
          summaryNotification.setVibrate(VIBRATION);
        }
        if (sound) {
          summaryNotification.setSound(soundUri);
        }

        nm.notify(GROUP_NOTIFICATION_ID, summaryNotification.build());
      }
    }

    if (nextNotificationTime < Long.MAX_VALUE) {
      scheduleAt(nextNotificationTime);
    }
  }

  @RequiresApi(Build.VERSION_CODES.N)
  static int getGroupNotificationCount(NotificationManager nm, String group) {
    StatusBarNotification[] notifications = nm.getActiveNotifications();
    int count = 0;
    for (StatusBarNotification statusBarNotification : notifications) {
      final Notification n = statusBarNotification.getNotification();
      if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != Notification.FLAG_GROUP_SUMMARY
          && group.equals(n.getGroup())) {
        count++;
      }
    }
    return count;
  }

  private void displayNotification(long showId, String showTitle, long episodeId,
      String episodeTitle, int season, int episode, boolean watched, long firstAired,
      boolean airing) {
    final String epiTitle =
        DataHelper.getEpisodeTitle(this, episodeTitle, season, episode, watched, true);

    final String contentTitle = showTitle + " - " + epiTitle;

    String contentText;
    String tickerText;

    final String time = timeDateFormat.format(new Date(firstAired));

    if (airing) {
      contentText = getString(R.string.notification_aired_at, time);
      tickerText = getString(R.string.notification_show_aired_at, showTitle, time);
    } else {
      contentText = getString(R.string.notification_airing_at, time);
      tickerText = getString(R.string.notification_show_airing_at, showTitle, time);
    }

    final int notificationId = Longs.hashCode(episodeId);

    NotificationCompat.Builder notification =
        new NotificationCompat.Builder(this).setShowWhen(false)
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
            .setGroup(GROUP);

    // Check-in action
    Intent checkInIntent = new Intent(this, NotificationActionReceiver.class);
    checkInIntent.setAction(NotificationActionReceiver.ACTION_CHECK_IN);
    checkInIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
    checkInIntent.putExtra(NotificationActionReceiver.EXTRA_ID, episodeId);
    checkInIntent.setData(Episodes.withId(episodeId));
    PendingIntent checkInPI = PendingIntent.getBroadcast(this, 0, checkInIntent, 0);
    final String checkIn = getString(R.string.action_checkin);
    notification.addAction(0, checkIn, checkInPI);

    // Delete intent
    Intent dismissIntent = new Intent(this, NotificationActionReceiver.class);
    dismissIntent.setAction(NotificationActionReceiver.ACTION_DISMISS);
    dismissIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
    dismissIntent.putExtra(NotificationActionReceiver.EXTRA_ID, episodeId);
    dismissIntent.setData(Episodes.withId(episodeId));
    PendingIntent dismissPI = PendingIntent.getBroadcast(this, 0, dismissIntent, 0);
    notification.setDeleteIntent(dismissPI);

    // Content intent
    Intent contentIntent = new Intent(this, EpisodeDetailsActivity.class);
    contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_ID, episodeId);
    contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_SHOW_ID, showId);
    contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_SHOW_TITLE, showTitle);
    contentIntent.setData(Episodes.withId(episodeId));
    PendingIntent contentPI =
        PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    notification.setContentIntent(contentPI);

    NotificationManagerCompat nm = NotificationManagerCompat.from(this);
    nm.notify(notificationId, notification.build());
  }

  private static String createSortKey(long firstAired) {
    return SORT_KEY_FORMAT.format(firstAired);
  }

  private void scheduleAt(long millis) {
    Intent i = new Intent(this, NotificationReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
    Alarms.setExactAndAllowWhileIdle(this, AlarmManager.RTC_WAKEUP, millis, pi);
  }
}
