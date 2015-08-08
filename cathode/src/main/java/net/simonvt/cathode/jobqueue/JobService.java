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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.remote.FourOhFourException;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.HomeActivity;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public class JobService extends Service {

  private static final String WAKELOCK_TAG = "net.simonvt.cathode.sync.TraktTaskService";

  static final String RETRY_DELAY = "net.simonvt.cathode.sync.TraktTaskService.retryDelay";

  private static final int MAX_RETRY_DELAY = 60; // In minutes

  private static final int NOTIFICATION_FOREGROUND = 42;
  private static final int NOTIFICATION_FAILURE = 43;

  private static volatile PowerManager.WakeLock sWakeLock = null;

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject JobManager jobManager;

  @Inject Bus bus;

  private volatile int retryDelay = -1;

  private boolean displayNotification;

  private Handler handler;

  private Looper looper;

  private boolean running;

  private static PowerManager.WakeLock getLock(Context context) {
    if (sWakeLock == null) {
      PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      sWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
    }

    return sWakeLock;
  }

  static boolean hasLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    return lock.isHeld();
  }

  static void acquireLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    if (!lock.isHeld()) {
      Timber.d("Acquiring wakelock");
      lock.acquire();
    }
  }

  static void releaseLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    if (lock.isHeld()) {
      Timber.d("Releasing wakelock");
      lock.release();
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    Timber.d("JobService started");
    CathodeApp.inject(this);

    acquireLock(this);

    cancelAlarm();

    HandlerThread thread = new HandlerThread("JobService");
    thread.start();

    looper = thread.getLooper();
    handler = new Handler(looper);

    bus.register(this);

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
  }

  private class JobRunnable implements Runnable {

    private Job job;

    private JobRunnable(Job job) {
      this.job = job;
    }

    @Override public void run() {
      if (job.requiresWakelock()) {
        acquireLock(JobService.this);
      } else {
        releaseLock(JobService.this);
      }

      try {
        Timber.d("Executing job: %s", job.getClass().getSimpleName());
        job.perform();
        jobFinished(job);
      } catch (Throwable t) {
        if (!(t instanceof RetrofitError)) {
          Timber.i("%s", job.key());
          Timber.e(t, "Unable to execute job");
        }

        jobFailed(job, t);
      }
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

    executeNext();

    return START_STICKY;
  }

  void jobFinished(Job job) {
    retryDelay = 1;
    jobManager.jobDone(job);

    if (!jobManager.hasJobs()) {
      stopSelf();
    } else {
      MAIN_HANDLER.post(new Runnable() {
        @Override public void run() {
          running = false;
          executeNext();
        }
      });
    }
  }

  private void executeNext() {
    Timber.d("[executeNext]");
    if (running) {
      Timber.d("[executeNext] already running");
      return;
    }

    running = true;

    Job nextJob = jobManager.nextJob();
    if (nextJob != null) {
      handler.post(new JobRunnable(nextJob));
    } else {
      running = false;
      stopSelf();
    }
  }

  void jobFailed(final Job job, final Throwable t) {
    jobManager.jobFailed(job);

    MAIN_HANDLER.post(new Runnable() {
      @Override public void run() {
        running = false;

        if (handleError(job, t)) {
          scheduleAlarm();
          stopSelf();
        }
      }
    });
  }

  public boolean handleError(Job job, Throwable t) {
    int retryDelay = Math.max(1, this.retryDelay);

    String contentText = null;

    if (t instanceof RetrofitError) {
      RetrofitError error = (RetrofitError) t;
      switch (error.getKind()) {
        case HTTP:
          Response response = error.getResponse();
          if (response != null) {
            final int statusCode = response.getStatus();
            if (statusCode == 401) {
              // Notification is created elsewhere
              return true;
            } else if (statusCode == 404) {
              Timber.i("%s", job.key());
              Timber.e(new FourOhFourException(t), "404");
              jobManager.jobDone(job);
              executeNext();
              return false;
            } else if (statusCode >= 500 && statusCode < 600) {
              contentText = getString(R.string.error_5xx_retry_in, retryDelay);
            } else {
              contentText = getString(R.string.error_unknown_retry_in, retryDelay);
            }
          } else {
            contentText = getString(R.string.error_unknown_retry_in, retryDelay);
          }
          break;

        case CONVERSION:
        case UNEXPECTED:
          contentText = getString(R.string.error_unknown_retry_in, retryDelay);
          break;

        case NETWORK:
          contentText = getString(R.string.error_unknown_retry_in, retryDelay);
          break;
      }
    } else {
      contentText = getString(R.string.error_unknown_retry_in, retryDelay);
    }

    if (displayNotification) {
      Intent clickIntent = new Intent(JobService.this, HomeActivity.class);
      PendingIntent clickPi = PendingIntent.getActivity(JobService.this, 0, clickIntent, 0);

      Notification.Builder builder = new Notification.Builder(JobService.this) //
          .setSmallIcon(R.drawable.ic_notification)
          .setTicker(getString(R.string.lost_connection))
          .setContentTitle(getString(R.string.lost_connection, retryDelay))
          .setContentIntent(clickPi)
          .setContentText(contentText)
          .setStyle(new Notification.BigTextStyle().bigText(contentText));

      NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      nm.notify(NOTIFICATION_FAILURE, builder.build());
    }

    return true;
  }

  @Produce public SyncEvent provideRunningEvent() {
    return new SyncEvent(true);
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    bus.post(new SyncEvent(false));

    looper.quit();

    if (displayNotification && !jobManager.hasJobs()) {
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
        ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras);
      }
    }

    releaseLock(this);

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
