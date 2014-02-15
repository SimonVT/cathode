/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote;

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
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.AccountService;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.SyncEvent;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.HomeActivity;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktTaskService extends Service implements TraktTask.TaskCallback {

  private static final String TAG = "TraktTaskService";
  private static final String WAKELOCK_TAG = "net.simonvt.cathode.sync.TraktTaskService";

  static final String RETRY_DELAY = "net.simonvt.cathode.sync.TraktTaskService.retryDelay";

  public static final String ACTION_LOGOUT = "net.simonvt.cathode.sync.TraktTaskService.LOGOUT";

  private static final int MAX_RETRY_DELAY = 60;

  private static final int NOTIFICATION_ID = 42;

  private static volatile PowerManager.WakeLock sWakeLock = null;

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject TraktTaskQueue queue;
  @Inject @PriorityQueue TraktTaskQueue priorityQueue;

  @Inject AccountService accountService;

  @Inject Bus bus;

  private volatile boolean running;

  private boolean executingPriorityTask;

  private int retryDelay = -1;

  private boolean displayNotification;

  private volatile boolean logout;

  private HandlerThread thread;

  private Handler handler;

  private static PowerManager.WakeLock getLock(Context context) {
    if (sWakeLock == null) {
      PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      sWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
    }

    return sWakeLock;
  }

  static void acquireLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    if (!lock.isHeld()) {
      lock.acquire();
    }
  }

  static void releaseLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    if (lock.isHeld()) {
      lock.release();
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    acquireLock(this);
    CathodeApp.inject(this);
    bus.register(this);

    thread = new HandlerThread("TaskQueue");
    thread.start();
    handler = new Handler(thread.getLooper());

    cancelAlarm();

    displayNotification =
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Settings.INITIAL_SYNC, true);

    if (displayNotification) {
      Timber.d("Display notification");
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
      startForeground(NOTIFICATION_ID, builder.build());
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (retryDelay == -1) {
      if (intent != null) {
        retryDelay = intent.getIntExtra(RETRY_DELAY, -1);
      }
    }
    String action = null;
    if (intent != null) action = intent.getAction();
    if (ACTION_LOGOUT.equals(action)) {
      Timber.tag(TAG).i("Logging out");
      logout();
    } else if (!running) {
      // Check authentication before executing tasks
      checkAuth();
    }

    return START_STICKY;
  }

  @Override public void onDestroy() {
    Timber.d("onDestroy");
    thread.quit();
    bus.unregister(this);
    bus.post(new SyncEvent(false));
    releaseLock(this);
    super.onDestroy();
  }

  private void checkAuth() {
    running = true;
    new Thread(new Runnable() {
      @Override public void run() {
        try {
          Timber.d("Checking authentication");
          Response response = accountService.test();

          if (response == null || "failed authentication".equals(response.getError())) {
            MAIN_HANDLER.post(new Runnable() {
              @Override public void run() {
                Timber.i("Authentication failed");
                bus.post(new AuthFailedEvent());
                running = false;
                stopSelf();
              }
            });
          } else {
            Timber.d("[Auth Check] " + response.getMessage());
            MAIN_HANDLER.post(new Runnable() {
              @Override public void run() {
                running = false;
                executeNext();
              }
            });
          }
        } catch (RetrofitError error) {
          MAIN_HANDLER.post(new Runnable() {
            @Override public void run() {
              onFailure();
            }
          });
        }
      }
    }).start();
  }

  private void logout() {
    logout = true;

    TraktTask priorityTask = priorityQueue.peek();
    TraktTask task = queue.peek();
    if (priorityTask != null) priorityTask.cancel();
    if (task != null) task.cancel();

    handler.post(new Runnable() {
      @Override public void run() {
        priorityQueue.clear();
        queue.clear();
        CathodeDatabase.getInstance(TraktTaskService.this).clearUserData();

        MAIN_HANDLER.post(new Runnable() {
          @Override public void run() {
            logout = false;
            running = false;
            executingPriorityTask = false;
            TraktTask task = getNextTask();
            if (task != null) {
              running = true;
              handler.post(new TaskRunnable(task, TraktTaskService.this));
            } else {
              Timber.d("Stopping from logout");
              stopSelf();
            }
          }
        });
      }
    });
  }

  @Produce public SyncEvent produceSyncEvent() {
    return new SyncEvent(true);
  }

  private void executeNext() {
    if (running) return; // Only one task at a time.

    running = true;

    TraktTask task = getNextTask();

    if (task != null) {
      handler.post(new TaskRunnable(task, this));
    } else {
      if (displayNotification) {
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

      stopSelf();
    }
  }

  private TraktTask getNextTask() {
    TraktTask task = priorityQueue.peek();
    if (task != null) {
      executingPriorityTask = true;
    } else {
      task = queue.peek();
    }

    return task;
  }

  private static class TaskRunnable implements Runnable {

    private TraktTask task;

    private TraktTaskService service;

    private TaskRunnable(TraktTask task, TraktTaskService service) {
      this.task = task;
      this.service = service;
    }

    @Override public void run() {
      task.execute(service, service);
    }
  }

  @Override public void onSuccess() {
    if (!logout) {
      running = false;

      if (executingPriorityTask) {
        executingPriorityTask = false;
        priorityQueue.remove();
      } else {
        queue.remove();
      }

      executeNext();
    }
  }

  @Override public void onFailure() {
    if (!logout) {
      Timber.d("Task failed, scheduling restart");
      running = false;

      scheduleAlarm();

      if (displayNotification) {
        showSyncFailNotification();
      }

      stopSelf();
    }
  }

  private void cancelAlarm() {
    Intent intent = new Intent(this, TaskServiceReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    am.cancel(pi);
  }

  private void scheduleAlarm() {
    Intent intent = new Intent(this, TaskServiceReceiver.class);
    final int retryDelay = Math.max(1, this.retryDelay);
    final int nextDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY);
    intent.putExtra(RETRY_DELAY, nextDelay);

    PendingIntent pi =
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    final long runAt = SystemClock.elapsedRealtime() + retryDelay * DateUtils.MINUTE_IN_MILLIS;
    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);
  }

  private void showSyncFailNotification() {
    Intent clickIntent = new Intent(this, HomeActivity.class);
    PendingIntent clickPi = PendingIntent.getActivity(this, 0, clickIntent, 0);

    Notification.Builder builder = new Notification.Builder(this) //
        .setSmallIcon(R.drawable.ic_notification)
        .setTicker(getString(R.string.lost_connection))
        .setContentTitle(getString(R.string.retry_in, retryDelay))
        .setContentIntent(clickPi)
        .setAutoCancel(true);

    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    nm.notify(NOTIFICATION_ID, builder.build());
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }
}
