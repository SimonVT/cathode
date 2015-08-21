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
package net.simonvt.cathode;

import android.accounts.Account;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;
import javax.inject.Inject;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.LogoutEvent;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.ForceUpdateJob;
import net.simonvt.cathode.remote.LogoutJob;
import net.simonvt.cathode.remote.UpdateShowCounts;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.LoginActivity;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.MainHandler;
import timber.log.Timber;

public class CathodeApp extends Application {

  private static final int AUTH_NOTIFICATION = 2;

  private static final long SYNC_DELAY = 15 * DateUtils.MINUTE_IN_MILLIS;

  private SharedPreferences settings;

  private ObjectGraph objectGraph;

  @Inject Bus bus;

  @Inject JobManager jobManager;

  private int homeActivityResumedCount;
  private long lastSync;

  @Override public void onCreate() {
    super.onCreate();
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());

      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
      StrictMode.setThreadPolicy(
          new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().penaltyLog().build());
    } else {
      Fabric.with(this, new Crashlytics());
      Timber.plant(new CrashlyticsTree());
    }

    settings = PreferenceManager.getDefaultSharedPreferences(this);

    upgrade();

    objectGraph = ObjectGraph.create(Modules.list(this));
    objectGraph.inject(this);

    bus.register(this);

    final boolean isLoggedIn = settings.getBoolean(Settings.TRAKT_LOGGED_IN, false);
    final boolean accountExists = Accounts.accountExists(this);
    if (isLoggedIn && !accountExists) {
      final String username = settings.getString(Settings.Profile.USERNAME, null);
      Accounts.setupAccount(this, username);
    } else if (!isLoggedIn && accountExists) {
      Accounts.removeAccount(this);
    }

    registerActivityLifecycleCallbacks(new SimpleActivityLifecycleCallbacks() {

      @Override public void onActivityResumed(Activity activity) {
        if (activity instanceof HomeActivity) {
          homeResumed();
        }
      }

      @Override public void onActivityPaused(Activity activity) {
        if (activity instanceof HomeActivity) {
          homePaused();
        }
      }
    });
  }

  private Runnable syncRunnable = new Runnable() {
    @Override public void run() {
      Timber.d("Performing periodic sync");
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CathodeApp.this);
      final long lastFullSync = settings.getLong(Settings.LAST_FULL_SYNC, 0);
      final long currentTime = System.currentTimeMillis();
      if (lastFullSync + 24 * DateUtils.DAY_IN_MILLIS < currentTime) {
        jobManager.addJob(new SyncJob());
      } else {
        // TODO: jobManager.addJob(new SyncActivityStreamTask());
        jobManager.addJob(new SyncUserActivity());
      }
      lastSync = System.currentTimeMillis();
      MainHandler.postDelayed(this, SYNC_DELAY);
    }
  };

  private void homeResumed() {
    if (homeActivityResumedCount > 0) {
      final String message = "More than one HomeActivity resumed: " + homeActivityResumedCount;
      Timber.e(new Exception(message), message);
    }

    homeActivityResumedCount++;

    if (homeActivityResumedCount == 1) {
      Timber.d("Starting periodic sync");
      final long currentTime = System.currentTimeMillis();
      if (lastSync + SYNC_DELAY < currentTime) {
        syncRunnable.run();
      } else {
        final long delay = Math.max(SYNC_DELAY - (currentTime - lastSync), 0);
        MainHandler.postDelayed(syncRunnable, delay);
      }
    }
  }

  private void homePaused() {
    homeActivityResumedCount--;
    if (homeActivityResumedCount == 0) {
      Timber.d("Pausing periodic sync");
      MainHandler.removeCallbacks(syncRunnable);
    }
  }

  private void upgrade() {
    final int currentVersion = settings.getInt(Settings.VERSION_CODE, -1);

    if (currentVersion == -1) {
      settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
      return;
    }

    if (currentVersion != BuildConfig.VERSION_CODE) {
      if (currentVersion < 20002) {
        Accounts.removeAccount(this);
        settings.edit().clear().apply();
      }
      if (currentVersion < 20501) {
        TraktTimestamps.clear(this);
      }
      if (currentVersion < 21001) {
        MainHandler.post(new Runnable() {
          @Override public void run() {
            jobManager.addJob(new ForceUpdateJob());
          }
        });
      }
      if (currentVersion <= 21001) {
        MainHandler.post(new Runnable() {
          @Override public void run() {
            jobManager.addJob(new UpdateShowCounts());
          }
        });
      }
      if (currentVersion <= 31000) {
        Account account = Accounts.getAccount(this);

        ContentResolver.setIsSyncable(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, 1);
        ContentResolver.setSyncAutomatically(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, true);
        ContentResolver.addPeriodicSync(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, new Bundle(),
            12 * DateUtils.HOUR_IN_SECONDS);

        Accounts.requestCalendarSync(this);
      }

      MainHandler.post(new Runnable() {
        @Override public void run() {
          jobManager.addJob(new SyncJob());
        }
      });

      settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
    }
  }

  @Subscribe public void onAuthFailure(AuthFailedEvent event) {
    Timber.i("onAuthFailure");
    if (!Accounts.accountExists(this)) {
      // TODO: Try and make sure this doesn't happen
      return; // User has logged out, ignore.
    }

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    settings.edit().putBoolean(Settings.TRAKT_LOGGED_IN, false).apply();

    Intent intent = new Intent(this, LoginActivity.class);
    intent.setAction(HomeActivity.ACTION_LOGIN);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

    Notification.Builder builder = new Notification.Builder(this) //
        .setSmallIcon(R.drawable.ic_noti_error)
        .setTicker(getString(R.string.auth_failed))
        .setContentTitle(getString(R.string.auth_failed))
        .setContentText(getString(R.string.auth_failed_desc))
        .setContentIntent(pi)
        .setPriority(Notification.PRIORITY_HIGH)
        .setAutoCancel(true);

    NotificationManager nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    nm.notify(AUTH_NOTIFICATION, builder.build());
  }

  @Subscribe public void onLogout(LogoutEvent event) {
    Settings.clearUserSettings(this);
    TraktTimestamps.clear(this);

    jobManager.addJob(new LogoutJob());
    jobManager.removeJobsWithFlag(Flags.REQUIRES_AUTH);
  }

  public static void inject(Context context) {
    ((CathodeApp) context.getApplicationContext()).objectGraph.inject(context);
  }

  public static void inject(Context context, Object object) {
    ((CathodeApp) context.getApplicationContext()).objectGraph.inject(object);
  }
}
