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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import dagger.android.AndroidInjection;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.util.Longs;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.sync.trakt.CheckIn;
import net.simonvt.cathode.ui.EpisodeDetailsActivity;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public class NotificationActionService extends JobIntentService {

  static final int JOB_ID = 50;

  @Inject JobManager jobManager;
  @Inject EpisodeDatabaseHelper episodeHelper;
  @Inject CheckIn checkIn;

  static void enqueueWork(Context context, Intent work) {
    enqueueWork(context, NotificationActionService.class, JOB_ID, work);
  }

  @Override public void onCreate() {
    super.onCreate();
    AndroidInjection.inject(this);
  }

  @Override protected void onHandleWork(@NonNull Intent intent) {
    final String action = intent.getAction();
    int notificationId = intent.getIntExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, -1);
    final long id = intent.getLongExtra(NotificationActionReceiver.EXTRA_ID, -1L);

    switch (action) {
      case NotificationActionReceiver.ACTION_CHECK_IN: {
        Timber.d("Action check in");
        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationId > -1) {
          int notificationCount = -1;
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationCount =
                NotificationHelper.getGroupNotificationCount(nm, NotificationHelper.GROUP);
          }

          nm.cancel(notificationId);

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (notificationCount <= 1) {
              nm.cancel(NotificationHelper.GROUP_NOTIFICATION_ID);
            }
          }
        }

        if (!checkIn.episode(id, null, false, false, false)) {
          notificationId = Longs.hashCode(id);

          Cursor episode = getContentResolver().query(Episodes.withId(id), new String[] {
              EpisodeColumns.TITLE, EpisodeColumns.SHOW_ID, EpisodeColumns.SHOW_TITLE,
              EpisodeColumns.SEASON, EpisodeColumns.EPISODE, EpisodeColumns.WATCHED,
          }, null, null, null);
          episode.moveToFirst();
          final long showId = Cursors.getLong(episode, EpisodeColumns.SHOW_ID);
          final String showTitle = Cursors.getString(episode, EpisodeColumns.SHOW_TITLE);
          final int number = Cursors.getInt(episode, EpisodeColumns.EPISODE);
          final int season = Cursors.getInt(episode, EpisodeColumns.SEASON);
          final boolean watched = Cursors.getBoolean(episode, EpisodeColumns.WATCHED);
          final String episodeTitle =
              DataHelper.getEpisodeTitle(this, episode, season, number, watched, true);
          episode.close();

          Timber.d("Title: %s", episodeTitle);

          NotificationCompat.Builder notification =
              new NotificationCompat.Builder(this, CathodeApp.CHANNEL_ERRORS).setShowWhen(false)
                  .setContentTitle(getString(R.string.checkin_error_notification_title))
                  .setContentText(getString(R.string.checkin_error_notification_body, episodeTitle))
                  .setTicker(getString(R.string.checkin_error_notification_title))
                  .setSmallIcon(R.drawable.ic_notification)
                  .setAutoCancel(false)
                  .setPriority(NotificationCompat.PRIORITY_HIGH)
                  .setCategory(NotificationCompat.CATEGORY_ERROR)
                  .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                  .setLocalOnly(true);

          // Check-in action
          Intent checkInIntent = new Intent(this, NotificationActionReceiver.class);
          checkInIntent.setAction(NotificationActionReceiver.ACTION_CHECK_IN);
          checkInIntent.putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
          checkInIntent.putExtra(NotificationActionReceiver.EXTRA_ID, id);
          checkInIntent.setData(Episodes.withId(id));
          PendingIntent checkInPI = PendingIntent.getBroadcast(this, 0, checkInIntent, 0);
          final String checkIn = getString(R.string.action_checkin);
          notification.addAction(0, checkIn, checkInPI);

          // Content intent
          Intent contentIntent = new Intent(this, EpisodeDetailsActivity.class);
          contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_ID, id);
          contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_SHOW_ID, showId);
          contentIntent.putExtra(EpisodeDetailsActivity.EXTRA_SHOW_TITLE, showTitle);
          contentIntent.setData(Episodes.withId(id));
          PendingIntent contentPI =
              PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
          notification.setContentIntent(contentPI);

          nm.notify(notificationId, notification.build());
        }

        ContentValues values = new ContentValues();
        values.put(EpisodeColumns.NOTIFICATION_DISMISSED, true);
        getContentResolver().update(Episodes.withId(id), values, null, null);
        break;
      }

      case NotificationActionReceiver.ACTION_DISMISS: {
        Timber.d("Action dismiss");
        ContentValues values = new ContentValues();
        values.put(EpisodeColumns.NOTIFICATION_DISMISSED, true);
        getContentResolver().update(Episodes.withId(id), values, null, null);
        break;
      }

      default:
        throw new IllegalArgumentException("Unknown action: " + action);
    }
  }
}
