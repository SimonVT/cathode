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
import android.content.SharedPreferences;
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
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.UserToken;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.api.entity.RequestError;
import net.simonvt.cathode.api.entity.TokenRequest;
import net.simonvt.cathode.api.entity.UserSettings;
import net.simonvt.cathode.api.enumeration.GrantType;
import net.simonvt.cathode.api.service.AuthorizationService;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.event.SyncEvent;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.HomeActivity;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktTaskService extends Service implements TraktTask.TaskCallback {

  private static final String TAG = "TraktTaskService";
  private static final String WAKELOCK_TAG = "net.simonvt.cathode.sync.TraktTaskService";

  static final String RETRY_DELAY = "net.simonvt.cathode.sync.TraktTaskService.retryDelay";

  public static final String ACTION_LOGOUT = "net.simonvt.cathode.sync.TraktTaskService.LOGOUT";

  public static final String ACTION_GET_TOKEN =
      "net.simonvt.cathode.sync.TraktTaskService.GET_TOKEN";
  public static final String EXTRA_CODE = "net.simonvt.cathode.sync.TraktTaskService.CODE";

  private static final int MAX_RETRY_DELAY = 60;

  private static final int NOTIFICATION_ID = 42;

  private static volatile PowerManager.WakeLock sWakeLock = null;

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject TraktTaskQueue queue;
  @Inject @PriorityQueue TraktTaskQueue priorityQueue;

  @Inject AuthorizationService authorizationService;
  @Inject UsersService usersService;

  @Inject UserToken userToken;

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
    } else if (ACTION_GET_TOKEN.equals(action)) {
      String code = intent.getStringExtra(EXTRA_CODE);
      getToken(code);
    } else {
      executeNext();
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

  private void getToken(final String code) {
    running = true;

    new Thread(new Runnable() {
      @Override public void run() {
        try {
          final AccessToken token = authorizationService.getToken(
              new TokenRequest(code, BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_SECRET,
                  BuildConfig.TRAKT_REDIRECT_URL, GrantType.AUTHORIZATION_CODE));

          final String accessToken = token.getAccessToken();

          final SharedPreferences settings =
              PreferenceManager.getDefaultSharedPreferences(TraktTaskService.this);
          settings.edit().putString(Settings.TRAKT_TOKEN, accessToken).apply();

          userToken.setToken(accessToken);

          final UserSettings userSettings = usersService.getUserSettings();

          MAIN_HANDLER.post(new Runnable() {
            @Override public void run() {
              final String oldUsername = settings.getString(Settings.PROFILE_USERNAME, null);
              final String newUsername = userSettings.getUser().getUsername();
              if (newUsername != null && !newUsername.equals(oldUsername)) {
                Settings.clearProfile(TraktTaskService.this);
                // TODO: Clear oauth tasks
              }

              Settings.updateProfile(TraktTaskService.this, userSettings);

              running = false;
              executeNext();
            }
          });
        } catch (RetrofitError e) {
          RequestError error = (RequestError) e.getBodyAs(RequestError.class);
          if (error != null) {
            Timber.d(e, "Error: " + error.getError() + " - " + error.getErrorDescription());
          } else {
            Timber.d(e, "Request failed");
          }

          MAIN_HANDLER.post(new Runnable() {
            @Override public void run() {
              // TODO: Retry later
              running = false;
              stopSelf();
            }
          });
        }
      }
    }).start();
  }

  private void logout() {
    logout = true;

    userToken.setToken(null);
    TraktTimestamps.clear(this);

    TraktTask priorityTask = priorityQueue.peek();
    TraktTask task = queue.peek();
    if (priorityTask != null) priorityTask.cancel();
    if (task != null) task.cancel();

    handler.post(new Runnable() {
      @Override public void run() {
        priorityQueue.clear();
        queue.clear();
        DatabaseSchematic.clearUserData(TraktTaskService.this);

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
