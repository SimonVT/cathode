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

package net.simonvt.cathode.sync.jobqueue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.DateUtils;
import dagger.android.AndroidInjection;
import javax.inject.Inject;
import net.simonvt.cathode.common.util.WakeLock;
import timber.log.Timber;

public class AuthJobService extends Service {

  static final String WAKELOCK_TAG = "net.simonvt.cathode.sync.AuthJobService";

  static final String RETRY_DELAY = "net.simonvt.cathode.sync.AuthJobService.retryDelay";

  private static final long MAX_RETRY_DELAY = 5 * DateUtils.HOUR_IN_MILLIS;

  @Inject AuthJobHandler authJobHandler;

  private volatile long retryDelay = -1;

  private int startId;

  @Override public void onCreate() {
    super.onCreate();
    Timber.d("AuthJobService started");
    AndroidInjection.inject(this);

    WakeLock.acquire(this, WAKELOCK_TAG);

    authJobHandler.registerListener(handlerListener);
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    this.startId = startId;

    if (retryDelay == -1L) {
      if (intent != null) {
        long delay = intent.getLongExtra(RETRY_DELAY, -1L);
        if (delay != -1L) {
          retryDelay = delay;
        }
      }
    }

    if (!authJobHandler.hasJobs()) {
      stopSelfResult(startId);
    }

    return START_STICKY;
  }

  private JobHandler.JobHandlerListener handlerListener = new JobHandler.JobHandlerListener() {

    @Override public void onQueueEmpty() {
      Timber.d("AuthJobService queue empty");
      stopSelfResult(startId);
    }

    @Override public void onQueueFailed() {
      Timber.d("AuthJobService queue failed");
      scheduleAlarm();
      stopSelfResult(startId);
    }
  };

  private void scheduleAlarm() {
    Intent intent = new Intent(AuthJobService.this, AuthJobReceiver.class);
    final long retryDelay = Math.max(1, this.retryDelay);
    final long nextDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY);
    intent.putExtra(RETRY_DELAY, nextDelay);

    PendingIntent pi = PendingIntent.getBroadcast(AuthJobService.this, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    am.cancel(pi);

    final long runAt = SystemClock.elapsedRealtime() + retryDelay;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && retryDelay < 10) {
      am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);
    } else {
      am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);
    }

    Timber.d("Scheduling alarm in %d minutes", retryDelay);
  }

  @Override public void onDestroy() {
    Timber.d("AuthJobService stopped");
    authJobHandler.unregisterListener(handlerListener);
    WakeLock.release(this, WAKELOCK_TAG);
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }
}
