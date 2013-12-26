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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.HomeActivity;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktTaskService extends Service implements TraktTask.TaskCallback {

  private static final String TAG = "TraktTaskService";
  private static final String WAKELOCK_TAG = "net.simonvt.cathode.sync.TraktTaskService";

  private static final String RETRY_DELAY = "net.simonvt.cathode.sync.TraktTaskService.retryDelay";

  public static final String ACTION_LOGOUT = "net.simonvt.cathode.sync.TraktTaskService.LOGOUT";

  private static final int MAX_RETRY_DELAY = 60;

  private static final int NOTIFICATION_ID = 42;

  private static volatile PowerManager.WakeLock sWakeLock = null;

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject TraktTaskQueue priorityQueue;
  @Inject @PriorityQueue TraktTaskQueue queue;

  @Inject AccountService accountService;

  @Inject Bus bus;

  private boolean running;

  private boolean executingPriorityTask;

  private int retryDelay = -1;

  private boolean displayNotification;

  private boolean logout;

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

    Intent intent = new Intent(this, TaskServiceReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    am.cancel(pi);

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
        retryDelay = intent.getIntExtra(RETRY_DELAY, 1);
      } else {
        retryDelay = 1;
      }
    }
    String action = null;
    if (intent != null) action = intent.getAction();
    if (ACTION_LOGOUT.equals(action)) {
      TraktTask priorityTask = priorityQueue.peek();
      TraktTask task = queue.peek();
      if (priorityTask != null) priorityTask.cancel();
      if (task != null) task.cancel();

      priorityQueue.clear();
      queue.clear();

      if (running) {
        logout = true;
      } else {
        clearUserData();
      }
    } else if (!running) {
      // Check authentication before executing tasks
      running = true;
      new Thread(new Runnable() {
        @Override public void run() {
          try {
            Timber.d("Checking authentication");
            Response response = accountService.test();

            if ("failed authentication".equals(response.getError())) {
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

    return START_STICKY;
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    bus.post(new SyncEvent(false));
    releaseLock(this);
    super.onDestroy();
  }

  private void clearUserData() {
    running = true;
    new Thread(new Runnable() {
      @Override public void run() {
        ContentResolver resolver = getContentResolver();
        ContentValues cv;

        cv = new ContentValues();
        cv.put(CathodeContract.Shows.WATCHED_COUNT, 0);
        cv.put(CathodeContract.Shows.IN_COLLECTION_COUNT, 0);
        cv.put(CathodeContract.Shows.IN_WATCHLIST_COUNT, 0);
        cv.put(CathodeContract.Shows.IN_WATCHLIST, false);
        resolver.update(CathodeContract.Shows.CONTENT_URI, cv, null, null);

        cv = new ContentValues();
        cv.put(CathodeContract.Seasons.WATCHED_COUNT, 0);
        cv.put(CathodeContract.Seasons.IN_COLLECTION_COUNT, 0);
        cv.put(CathodeContract.Seasons.IN_WATCHLIST_COUNT, 0);
        resolver.update(CathodeContract.Seasons.CONTENT_URI, cv, null, null);

        cv = new ContentValues();
        cv.put(CathodeContract.Episodes.WATCHED, 0);
        cv.put(CathodeContract.Episodes.PLAYS, 0);
        cv.put(CathodeContract.Episodes.IN_WATCHLIST, 0);
        cv.put(CathodeContract.Episodes.IN_COLLECTION, 0);
        resolver.update(CathodeContract.Episodes.CONTENT_URI, cv, null, null);

        cv = new ContentValues();
        cv.put(CathodeContract.Movies.WATCHED, 0);
        cv.put(CathodeContract.Movies.IN_COLLECTION, 0);
        cv.put(CathodeContract.Movies.IN_WATCHLIST, 0);
        resolver.update(CathodeContract.Movies.CONTENT_URI, cv, null, null);

        MAIN_HANDLER.post(new Runnable() {
          @Override public void run() {
            running = false;
            executeNext();
          }
        });
      }
    }).start();
  }

  @Produce public SyncEvent produceSyncEvent() {
    return new SyncEvent(true);
  }

  private void executeNext() {
    if (running) return; // Only one task at a time.

    TraktTask priorityTask = priorityQueue.peek();

    if (priorityTask != null) {
      running = true;
      executingPriorityTask = true;
      priorityTask.execute(this);
    } else {
      TraktTask task = queue.peek();
      if (task != null) {
        Timber.d("Executing next");
        running = true;
        task.execute(this);
      } else {
        Timber.d("Stopping");

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

        stopSelf(); // No more tasks are present. Stop.
      }
    }
  }

  @Override public void onSuccess() {
    running = false;

    if (!logout) {
      if (executingPriorityTask) {
        executingPriorityTask = false;
        priorityQueue.remove();
      } else {
        queue.remove();
      }

      executeNext();
    } else {
      logout = false;
      clearUserData();
    }
  }

  @Override public void onFailure() {
    Timber.d("Task failed, scheduling restart");
    running = false;

    if (logout) {
      logout = false;
      clearUserData();
      return;
    }

    Intent intent = new Intent(this, TaskServiceReceiver.class);
    final int nextDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY);
    intent.putExtra(RETRY_DELAY, nextDelay);

    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    final long runAt = SystemClock.elapsedRealtime() + retryDelay * DateUtils.MINUTE_IN_MILLIS;
    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);

    // A logout might have caused the failure. Just in case, don't show the notification.
    if (displayNotification) {
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

    stopSelf();
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }
}
