/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.jobqueue;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.HomeActivity;
import timber.log.Timber;

public class JobService extends Service {

  static final String RETRY_DELAY = "net.simonvt.cathode.sync.TraktTaskService.retryDelay";

  private static final int MAX_RETRY_DELAY = 60; // In minutes

  private static final int NOTIFICATION_FOREGROUND = 42;
  private static final int NOTIFICATION_FAILURE = 43;

  @Inject JobManager jobManager;

  private volatile int retryDelay = -1;

  private boolean displayNotification;

  private JobExecutor executor;

  @Override public void onCreate() {
    super.onCreate();
    Timber.d("JobService started");
    CathodeApp.inject(this);
    CathodeApp.jobServiceStarted();

    cancelAlarm();

    displayNotification =
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Settings.INITIAL_SYNC, true);

    if (displayNotification) {
      Timber.d("Displaying initial sync notification");
      Intent clickIntent = new Intent(this, HomeActivity.class);
      PendingIntent clickPi = PendingIntent.getActivity(this, 0, clickIntent, 0);

      Notification.Builder builder = new Notification.Builder(this) //
          .setSmallIcon(R.drawable.ic_notification)
          .setTicker(getString(R.string.initial_sync))
          .setContentTitle(getString(R.string.initial_sync))
          .setContentText(getString(R.string.initial_sync_desc))
          .setContentIntent(clickPi)
          .setPriority(Notification.PRIORITY_LOW)
          .setProgress(0, 0, true)
          .setOngoing(true);
      startForeground(NOTIFICATION_FOREGROUND, builder.build());
    }

    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    nm.cancel(NOTIFICATION_FAILURE);

    if (jobManager.hasJobs(0, Flags.REQUIRES_AUTH)) {
      executor = new JobExecutor(jobManager, executorListener, 3, 0, Flags.REQUIRES_AUTH);
    } else {
      stopSelf();
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (retryDelay == -1) {
      if (intent != null) {
        int delay = intent.getIntExtra(RETRY_DELAY, -1);
        if (delay != -1) {
          retryDelay = delay;
        }
      }
    }

    return START_STICKY;
  }

  private JobExecutor.JobExecutorListener executorListener = new JobExecutor.JobExecutorListener() {

    @Override public void onQueueEmpty() {
      Timber.d("JobService queue empty");
      stopSelf();
    }

    @Override public void onQueueFailed() {
      Timber.d("JobService queue failed");

      scheduleAlarm();
      stopSelf();
    }
  };

  @Override public void onDestroy() {
    if (executor != null) {
      executor.destroy();
    }

    CathodeApp.jobServiceStopped();

    if (displayNotification && !jobManager.hasJobs(0, Flags.REQUIRES_AUTH)) {
      PreferenceManager.getDefaultSharedPreferences(this)
          .edit()
          .putBoolean(Settings.INITIAL_SYNC, false)
          .apply();

      Bundle extras = new Bundle();
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
      extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

      AccountManager am = AccountManager.get(this);
      Account[] accounts = am.getAccountsByType(getString(R.string.accountType));
      for (Account account : accounts) {
        ContentResolver.requestSync(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, extras);
      }
    }

    Timber.d("JobService stopped");
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }

  private void cancelAlarm() {
    Intent intent = new Intent(this, JobService.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    am.cancel(pi);
  }

  private void scheduleAlarm() {
    Intent intent = new Intent(JobService.this, JobReceiver.class);
    final int retryDelay = Math.max(1, this.retryDelay);
    final int nextDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY);
    intent.putExtra(RETRY_DELAY, nextDelay);

    PendingIntent pi =
        PendingIntent.getBroadcast(JobService.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    final long runAt = SystemClock.elapsedRealtime() + retryDelay * DateUtils.MINUTE_IN_MILLIS;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && retryDelay < 10) {
      am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);
    } else {
      am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);
    }

    Timber.d("Scheduling alarm in %d minutes", retryDelay);
  }
}
