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
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.remote.action.shows.CheckInEpisode;
import net.simonvt.cathode.util.WakeLock;
import timber.log.Timber;

public class NotificationActionService extends IntentService {

  static final String LOCK_TAG = "NotificationActionService";

  @Inject JobManager jobManager;

  @Inject EpisodeDatabaseHelper episodeHelper;

  public NotificationActionService() {
    super("NotificationActionService");
  }

  @Override public void onCreate() {
    super.onCreate();
    CathodeApp.inject(this);
  }

  @Override public void onDestroy() {
    WakeLock.release(this, LOCK_TAG);
    super.onDestroy();
  }

  @Override protected void onHandleIntent(Intent intent) {
    if (intent == null) {
      return;
    }

    final String action = intent.getAction();
    final int notificationId =
        intent.getIntExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, -1);
    final long id = intent.getLongExtra(NotificationActionReceiver.EXTRA_ID, -1L);

    switch (action) {
      case NotificationActionReceiver.ACTION_CHECK_IN: {
        Timber.d("Action check in");
        final long traktId = episodeHelper.getTraktId(id);
        CheckInEpisode checkInJob = new CheckInEpisode(traktId, null, false, false, false);
        jobManager.addJobNow(checkInJob);

        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationId > -1) {
          int notificationCount = -1;
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationCount =
                NotificationService.getGroupNotificationCount(nm, NotificationService.GROUP);
          }

          nm.cancel(notificationId);

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (notificationCount <= 1) {
              nm.cancel(NotificationService.GROUP_NOTIFICATION_ID);
            }
          }
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
