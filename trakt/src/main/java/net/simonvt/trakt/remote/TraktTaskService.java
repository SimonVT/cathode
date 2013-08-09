package net.simonvt.trakt.remote;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import javax.inject.Inject;
import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.event.SyncEvent;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.ui.HomeActivity;
import net.simonvt.trakt.util.LogWrapper;

public class TraktTaskService extends Service implements TraktTask.TaskCallback {

  private static final String TAG = "TraktTaskService";
  private static final String WAKELOCK_TAG = "net.simonvt.trakt.sync.TraktTaskService";

  private static final String RETRY_DELAY = "net.simonvt.trakt.sync.TraktTaskService.retryDelay";

  private static int MAX_RETRY_DELAY = 60;

  private static int NOTIFICATION_ID = 42;

  private static volatile PowerManager.WakeLock sWakeLock = null;

  @Inject PriorityTraktTaskQueue priorityQueue;
  @Inject TraktTaskQueue queue;

  @Inject Bus bus;

  private boolean running;

  private boolean executingPriorityTask;

  private int retryDelay = -1;

  private boolean displayNotification;

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

  public void onCreate() {
    super.onCreate();
    acquireLock(this);
    TraktApp.inject(this);
    bus.register(this);

    Intent intent = new Intent(this, TaskServiceReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    am.cancel(pi);

    displayNotification =
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Settings.INITIAL_SYNC, true);

    if (displayNotification) {
      LogWrapper.v(TAG, "Display notification");
      Intent clickIntent = new Intent(this, HomeActivity.class);
      PendingIntent clickPi = PendingIntent.getActivity(this, 0, clickIntent, 0);

      Notification.Builder builder = new Notification.Builder(this) //
          .setSmallIcon(R.drawable.ic_launcher)
          .setTicker(getString(R.string.initial_sync))
          .setContentTitle(getString(R.string.initial_sync))
          .setContentText(getString(R.string.initial_sync_desc))
          .setContentIntent(clickPi)
          .setProgress(0, 0, true)
          .setOngoing(true);
      startForeground(NOTIFICATION_ID, builder.build());
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (retryDelay == -1) {
      retryDelay = intent.getIntExtra(RETRY_DELAY, 1);
    }
    executeNext();
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    bus.unregister(this);
    bus.post(new SyncEvent(false));
    releaseLock(this);
    super.onDestroy();
  }

  @Produce
  public SyncEvent produceSyncEvent() {
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
        LogWrapper.i(TAG, "Execute next");
        running = true;
        task.execute(this);
      } else {
        LogWrapper.i(TAG, "Service stopping!");

        if (displayNotification) {
          PreferenceManager.getDefaultSharedPreferences(this)
              .edit()
              .putBoolean(Settings.INITIAL_SYNC, false)
              .apply();
        }

        stopSelf(); // No more tasks are present. Stop.
      }
    }
  }

  @Override
  public void onSuccess() {
    running = false;
    if (executingPriorityTask) {
      executingPriorityTask = false;
      priorityQueue.remove();
    } else {
      queue.remove();
    }
    executeNext();
  }

  @Override
  public void onFailure() {
    LogWrapper.i(TAG, "[onFailure] Scheduling restart in 10 minutes");
    running = false;

    Intent intent = new Intent(this, TaskServiceReceiver.class);
    final int nextDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY);
    intent.putExtra(RETRY_DELAY, nextDelay);

    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    final long runAt = retryDelay * DateUtils.MINUTE_IN_MILLIS;
    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);

    stopForeground(true);

    if (displayNotification) {
      Intent clickIntent = new Intent(this, HomeActivity.class);
      PendingIntent clickPi = PendingIntent.getActivity(this, 0, clickIntent, 0);

      Notification.Builder builder = new Notification.Builder(this) //
          .setSmallIcon(R.drawable.ic_launcher)
          .setTicker(getString(R.string.lost_connection))
          .setContentTitle(getString(R.string.retry_in, retryDelay))
          .setContentIntent(clickPi)
          .setAutoCancel(true);

      NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      nm.notify(NOTIFICATION_ID, builder.build());
    }

    stopSelf();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
