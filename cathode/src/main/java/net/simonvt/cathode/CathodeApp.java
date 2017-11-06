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

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentProvider;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.format.DateUtils;
import android.view.View;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasContentProviderInjector;
import dagger.android.HasFragmentInjector;
import dagger.android.HasServiceInjector;
import dagger.android.support.HasSupportFragmentInjector;
import javax.inject.Inject;
import net.simonvt.cathode.common.dagger.HasViewInjector;
import net.simonvt.cathode.common.event.AuthFailedEvent;
import net.simonvt.cathode.common.event.AuthFailedEvent.OnAuthFailedListener;
import net.simonvt.cathode.common.event.ItemsUpdatedEvent;
import net.simonvt.cathode.common.event.ItemsUpdatedEvent.OnItemsUpdatedListener;
import net.simonvt.cathode.common.util.MainHandler;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.notification.NotificationHelper;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncUserMovies;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncUserShows;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.Timestamps;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.login.LoginActivity;
import net.simonvt.cathode.sync.jobqueue.AuthJobHandler;
import net.simonvt.cathode.sync.jobqueue.DataJobHandler;
import net.simonvt.cathode.sync.jobqueue.JobHandler;
import net.simonvt.cathode.sync.jobscheduler.AuthJobHandlerJob;
import net.simonvt.cathode.sync.jobscheduler.DataJobHandlerJob;
import net.simonvt.cathode.sync.jobscheduler.Jobs;
import net.simonvt.cathode.ui.HomeActivity;
import timber.log.Timber;

public class CathodeApp extends Application
    implements HasActivityInjector, HasFragmentInjector, HasSupportFragmentInjector, HasServiceInjector,
    HasViewInjector, HasContentProviderInjector {

  public static final String CHANNEL_ERRORS = "channel_errors";

  private static final int AUTH_NOTIFICATION = 2;

  private static final long SYNC_DELAY = 15 * DateUtils.MINUTE_IN_MILLIS;

  private int homeActivityResumedCount;
  private long lastSync;

  private AppComponent appComponent;
  private CathodeComponent cathodeComponent;

  @Inject AuthJobHandler authJobHandler;
  @Inject DataJobHandler dataJobHandler;
  @Inject JobManager jobManager;

  private volatile boolean injected = false;
  @Inject DispatchingAndroidInjector<Activity> activityInjector;
  @Inject DispatchingAndroidInjector<android.app.Fragment> fragmentInjector;
  @Inject DispatchingAndroidInjector<Fragment> supportFragmentInjector;
  @Inject DispatchingAndroidInjector<Service> serviceInjector;
  @Inject DispatchingAndroidInjector<ContentProvider> contentProviderInjector;
  @Inject DispatchingAndroidInjector<Job> jobInjector;
  @Inject DispatchingAndroidInjector<View> viewInjector;

  @Override public void onCreate() {
    super.onCreate();
    ensureInjection();

    AuthFailedEvent.registerListener(authFailedListener);

    if (TraktLinkSettings.isLinked(this)) {
      Accounts.setupAccount(this);
    } else {
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

    ItemsUpdatedEvent.registerListener(onItemsUpdatedListener);

    if (Jobs.usesScheduler()) {
      SyncUpdatedShows.schedulePeriodic(this);
      SyncUserShows.schedulePeriodic(this);
      SyncUpdatedMovies.schedulePeriodic(this);
      SyncUserMovies.schedulePeriodic(this);
      DataJobHandlerJob.schedulePeriodic(this);
      if (TraktLinkSettings.isLinked(this)) {
        AuthJobHandlerJob.schedulePeriodic(this);
        SyncUserActivity.schedulePeriodic(this);
      } else {
        AuthJobHandlerJob.cancel(this);
        SyncUserActivity.cancel(this);
      }
    }
  }

  public void ensureInjection() {
    if (!injected) {
      synchronized (this) {
        if (!injected) {
          appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
          cathodeComponent = appComponent.plusCathodeComponent();
          cathodeComponent.inject(this);
          injected = true;
        }
      }
    }
  }

  private Runnable syncRunnable = new Runnable() {
    @Override public void run() {
      Timber.d("Performing periodic sync");
      final long currentTime = System.currentTimeMillis();
      final long lastFullSync =
          Timestamps.get(CathodeApp.this).getLong(Timestamps.LAST_FULL_SYNC, 0);

      if (lastFullSync + DateUtils.DAY_IN_MILLIS < currentTime) {
        jobManager.addJob(new SyncJob());
      } else {
        jobManager.addJob(new SyncUserActivity());
        jobManager.addJob(new SyncWatching());
      }

      lastSync = System.currentTimeMillis();
      MainHandler.postDelayed(this, SYNC_DELAY);
    }
  };

  private void homeResumed() {
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

      authJobHandler.registerListener(authJobListener);
      dataJobHandler.registerListener(dataJobListener);
    }
  }

  private void homePaused() {
    homeActivityResumedCount--;
    if (homeActivityResumedCount == 0) {
      Timber.d("Pausing periodic sync");
      MainHandler.removeCallbacks(syncRunnable);
      authJobHandler.unregisterListener(authJobListener);
      dataJobHandler.unregisterListener(dataJobListener);
    }
  }

  private JobHandler.JobHandlerListener authJobListener = new JobHandler.JobHandlerListener() {

    @Override public void onQueueEmpty() {
      Timber.d("Auth job queue empty");
    }

    @Override public void onQueueFailed() {
      Timber.d("Auth job queue failed");
    }
  };

  private JobHandler.JobHandlerListener dataJobListener = new JobHandler.JobHandlerListener() {

    @Override public void onQueueEmpty() {
      Timber.d("Data job queue empty");
    }

    @Override public void onQueueFailed() {
      Timber.d("Data job queue failed");
    }
  };

  private OnItemsUpdatedListener onItemsUpdatedListener = new OnItemsUpdatedListener() {
    @Override public void onItemsUpdated() {
      NotificationHelper.schedule(CathodeApp.this,
          System.currentTimeMillis() + 5 * DateUtils.MINUTE_IN_MILLIS);
    }
  };

  private OnAuthFailedListener authFailedListener = new OnAuthFailedListener() {
    @Override public void onAuthFailed() {
      Timber.i("onAuthFailure");
      Settings.get(CathodeApp.this)
          .edit()
          .putBoolean(TraktLinkSettings.TRAKT_AUTH_FAILED, true)
          .apply();

      Intent intent = new Intent(CathodeApp.this, LoginActivity.class);
      intent.setAction(HomeActivity.ACTION_LOGIN);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      PendingIntent pi = PendingIntent.getActivity(CathodeApp.this, 0, intent, 0);

      createAuthChannel();

      NotificationCompat.Builder builder =
          new NotificationCompat.Builder(CathodeApp.this, CHANNEL_ERRORS) //
              .setSmallIcon(R.drawable.ic_noti_error)
              .setTicker(getString(R.string.auth_failed))
              .setContentTitle(getString(R.string.auth_failed))
              .setContentText(getString(R.string.auth_failed_desc))
              .setContentIntent(pi)
              .setPriority(Notification.PRIORITY_HIGH)
              .setAutoCancel(true);

      NotificationManagerCompat nm = NotificationManagerCompat.from(CathodeApp.this);
      nm.notify(AUTH_NOTIFICATION, builder.build());
    }
  };

  private void createAuthChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      final String name = getString(R.string.channel_errors);
      NotificationChannel channel =
          new NotificationChannel(CHANNEL_ERRORS, name, NotificationManager.IMPORTANCE_HIGH);
      channel.enableLights(true);
      channel.enableVibration(true);
      nm.createNotificationChannel(channel);
    }
  }

  @Override public AndroidInjector<Activity> activityInjector() {
    return activityInjector;
  }

  @Override public AndroidInjector<android.app.Fragment> fragmentInjector() {
    return fragmentInjector;
  }

  @Override public AndroidInjector<Fragment> supportFragmentInjector() {
    return supportFragmentInjector;
  }

  @Override public AndroidInjector<Service> serviceInjector() {
    return serviceInjector;
  }

  @Override public AndroidInjector<View> viewInjector() {
    return viewInjector;
  }

  @Override public AndroidInjector<ContentProvider> contentProviderInjector() {
    ensureInjection();
    return contentProviderInjector;
  }
}
