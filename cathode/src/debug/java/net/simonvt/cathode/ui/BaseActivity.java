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
package net.simonvt.cathode.ui;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.format.DateUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.HttpStatusCode;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.IntPreference;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.TraktSettings;
import net.simonvt.cathode.common.event.AuthFailedEvent;
import net.simonvt.cathode.common.event.RequestFailedEvent;
import net.simonvt.cathode.common.event.SyncEvent;
import net.simonvt.cathode.common.event.SyncEvent.OnSyncListener;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobListener;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.jobscheduler.AuthJobHandlerJob;
import net.simonvt.cathode.jobscheduler.DataJobHandlerJob;
import net.simonvt.cathode.jobscheduler.SchedulerService;
import net.simonvt.cathode.notification.NotificationService;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.ForceUpdateJob;
import net.simonvt.cathode.remote.InitialSyncJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.lists.SyncLists;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.StartPage;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.tmdb.api.SyncConfiguration;
import net.simonvt.cathode.widget.PaletteTransformation;
import okhttp3.logging.HttpLoggingInterceptor;

@SuppressLint("SetTextI18n") public abstract class BaseActivity extends AppCompatActivity {

  private static final long FAKE_SHOW_ID = Long.MAX_VALUE;

  final DebugViews debugViews = new DebugViews();

  final DebugInjects injects = new DebugInjects();

  private SharedPreferences settings;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    Injector.inject(this);
    Injector.inject(injects);

    super.setContentView(R.layout.debug_home);

    settings = PreferenceManager.getDefaultSharedPreferences(this);

    Context drawerContext = new ContextThemeWrapper(this, R.style.Theme_AppCompat);
    LayoutInflater.from(drawerContext)
        .inflate(R.layout.debug_drawer, (ViewGroup) ButterKnife.findById(this, R.id.debug_drawer));

    ButterKnife.bind(debugViews, this);

    SyncEvent.registerListener(debugSyncListener);

    final StartPageAdapter startPageAdapter = new StartPageAdapter();
    debugViews.startPage.setAdapter(startPageAdapter);
    debugViews.startPage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        StartPage startPage = startPageAdapter.getItem(position);
        settings.edit().putString(Settings.START_PAGE, startPage.toString()).apply();
      }

      @Override public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    StartPage startPage =
        StartPage.fromValue(settings.getString(Settings.START_PAGE, null), StartPage.DASHBOARD);
    debugViews.startPage.setSelection(startPageAdapter.getPositionForValue(startPage));

    debugViews.recreateActivity.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        recreate();
      }
    });

    debugViews.updateNotifications.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intent i = new Intent(BaseActivity.this, NotificationService.class);
        startService(i);
      }
    });

    debugViews.playImages.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        PaletteTransformation.shouldTransform = isChecked;
      }
    });

    debugViews.requestFailedEvent.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        RequestFailedEvent.post(R.string.error_unknown_retrying);
        debugViews.drawerLayout.closeDrawers();
      }
    });

    debugViews.authFailedEvent.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        AuthFailedEvent.post();
        debugViews.drawerLayout.closeDrawers();
      }
    });

    debugViews.removeAccessToken.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        settings.edit().remove(TraktLinkSettings.TRAKT_ACCESS_TOKEN).apply();
      }
    });

    debugViews.removeRefreshToken.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        settings.edit().remove(TraktLinkSettings.TRAKT_REFRESH_TOKEN).apply();
      }
    });

    debugViews.invalidateAccessToken.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        settings.edit()
            .putString(TraktLinkSettings.TRAKT_ACCESS_TOKEN, "invalid token")
            .putLong(TraktLinkSettings.TRAKT_TOKEN_EXPIRATION, 0L)
            .apply();
      }
    });

    debugViews.invalidateRefreshToken.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        settings.edit().putString(TraktLinkSettings.TRAKT_REFRESH_TOKEN, "invalid token").apply();
      }
    });

    debugViews.insertFakeShow.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        new Thread(new Runnable() {
          @Override public void run() {
            long showId = injects.showHelper.getId(FAKE_SHOW_ID);
            if (showId > -1L) {
              getContentResolver().delete(Shows.withId(showId), null, null);
            }

            ContentValues values = new ContentValues();
            values.put(ShowColumns.TITLE, "Fake show");
            values.put(ShowColumns.TRAKT_ID, FAKE_SHOW_ID);
            values.put(ShowColumns.RUNTIME, 1);
            Uri showUri = getContentResolver().insert(Shows.SHOWS, values);
            showId = Shows.getShowId(showUri);

            values = new ContentValues();
            values.put(SeasonColumns.SHOW_ID, showId);
            values.put(SeasonColumns.SEASON, 1);
            Uri seasonUri = getContentResolver().insert(Seasons.SEASONS, values);
            long seasonId = Seasons.getId(seasonUri);

            long time = System.currentTimeMillis() - DateUtils.MINUTE_IN_MILLIS;

            for (int i = 1; i <= 10; i++) {
              values = new ContentValues();
              values.put(EpisodeColumns.SHOW_ID, showId);
              values.put(EpisodeColumns.SEASON_ID, seasonId);
              values.put(EpisodeColumns.SEASON, 1);
              values.put(EpisodeColumns.EPISODE, i);
              values.put(EpisodeColumns.FIRST_AIRED, time);
              values.put(EpisodeColumns.WATCHED, i == 1);
              getContentResolver().insert(Episodes.EPISODES, values);

              time += DateUtils.MINUTE_IN_MILLIS;
            }
          }
        }).start();
      }
    });

    debugViews.removeFakeShow.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final long showId = injects.showHelper.getId(FAKE_SHOW_ID);
        if (showId > -1L) {
          getContentResolver().delete(Shows.withId(showId), null, null);
        }
      }
    });

    final EnumAdapter<HttpLoggingInterceptor.Level> logLevelAdapter =
        new EnumAdapter(HttpLoggingInterceptor.Level.values());
    debugViews.logLevel.setAdapter(logLevelAdapter);
    debugViews.logLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        HttpLoggingInterceptor.Level logLevel = logLevelAdapter.getItem(position);
        injects.loggingInterceptor.setLevel(logLevel);
      }

      @Override public void onNothingSelected(AdapterView<?> adapterView) {
      }
    });
    debugViews.logLevel.setSelection(
        logLevelAdapter.getPositionForValue(injects.loggingInterceptor.getLevel()));

    int[] statusCodes = new int[] {
        200, 401, 404, 409, 412, 502,
    };
    final IntAdapter httpStatusCodeAdapter = new IntAdapter(statusCodes);
    debugViews.httpStatusCode.setAdapter(httpStatusCodeAdapter);
    debugViews.httpStatusCode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        injects.httpStatusCodePreference.set(httpStatusCodeAdapter.getItem(position));
      }

      @Override public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    debugViews.httpStatusCode.setSelection(
        httpStatusCodeAdapter.getPositionForValue(injects.httpStatusCodePreference.get()));

    debugViews.syncConfiguration.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.jobManager.addJob(new SyncConfiguration());
      }
    });

    debugViews.initialSync.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.jobManager.addJob(new InitialSyncJob());
      }
    });

    debugViews.forceUpdate.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.jobManager.addJob(new ForceUpdateJob());
      }
    });

    debugViews.updated.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.jobManager.addJob(new SyncUpdatedShows());
        injects.jobManager.addJob(new SyncUpdatedMovies());
      }
    });

    debugViews.syncWatching.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.jobManager.addJob(new SyncWatching());
      }
    });

    debugViews.syncLists.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.jobManager.addJob(new SyncLists());
      }
    });

    injects.jobManager.addJobListener(jobListener);

    debugViews.startPeriodicJobs.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
          JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

          JobInfo job = new JobInfo.Builder(SyncUpdatedShows.ID,
              new ComponentName(BaseActivity.this, SchedulerService.class))
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .build();
          scheduler.schedule(job);

          job = new JobInfo.Builder(SyncUpdatedMovies.ID,
              new ComponentName(BaseActivity.this, SchedulerService.class))
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .build();
          scheduler.schedule(job);

          job = new JobInfo.Builder(SyncUserActivity.ID,
              new ComponentName(BaseActivity.this, SchedulerService.class))
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .build();
          scheduler.schedule(job);

          job = new JobInfo.Builder(AuthJobHandlerJob.ID,
              new ComponentName(BaseActivity.this, SchedulerService.class))
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .build();
          scheduler.schedule(job);

          job = new JobInfo.Builder(DataJobHandlerJob.ID,
              new ComponentName(BaseActivity.this, SchedulerService.class))
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .build();
          scheduler.schedule(job);
        }
      }
    });
  }

  private JobListener jobListener = new JobListener() {
    @Override public void onJobsLoaded(JobManager jobManager) {
      debugViews.jobCount.setText(String.valueOf(jobManager.jobCount()));
    }

    @Override public void onJobAdded(JobManager jobManager, Job job) {
      debugViews.jobCount.setText(String.valueOf(jobManager.jobCount()));
    }

    @Override public void onJobRemoved(JobManager jobManager, Job job) {
      debugViews.jobCount.setText(String.valueOf(jobManager.jobCount()));
    }
  };

  @Override public void setContentView(int layoutResID) {
    debugViews.container.removeAllViews();
    getLayoutInflater().inflate(layoutResID, debugViews.container);
  }

  @Override protected void onDestroy() {
    injects.jobManager.removeJobListener(jobListener);
    SyncEvent.unregisterListener(debugSyncListener);
    super.onDestroy();
  }

  private OnSyncListener debugSyncListener = new OnSyncListener() {
    @Override public void onSyncChanged(boolean authExecuting, boolean dataExecuting) {
      if (authExecuting) {
        debugViews.authHandlerStatus.setText("Running");
      } else {
        debugViews.authHandlerStatus.setText("Stopped");
      }

      if (dataExecuting) {
        debugViews.dataHandlerStatus.setText("Running");
      } else {
        debugViews.dataHandlerStatus.setText("Stopped");
      }
    }
  };

  public static class DebugInjects {

    @Inject @HttpStatusCode IntPreference httpStatusCodePreference;

    @Inject JobManager jobManager;

    @Inject TraktSettings traktSettings;

    @Inject HttpLoggingInterceptor loggingInterceptor;

    @Inject ShowDatabaseHelper showHelper;
  }

  static class DebugViews {

    @BindView(R.id.debug_drawerLayout) DrawerLayout drawerLayout;

    @BindView(R.id.debug_content) ViewGroup container;

    @BindView(R.id.debug_startPage) Spinner startPage;

    @BindView(R.id.debug_recreateActivity) View recreateActivity;

    @BindView(R.id.debug_updateNotifications) View updateNotifications;

    @BindView(R.id.debug_playImages) SwitchCompat playImages;

    @BindView(R.id.debug_requestFailedEvent) View requestFailedEvent;

    @BindView(R.id.debug_authFailedEvent) View authFailedEvent;

    @BindView(R.id.debug_removeAccessToken) View removeAccessToken;

    @BindView(R.id.debug_removeRefreshToken) View removeRefreshToken;

    @BindView(R.id.debug_invalidateAccessToken) View invalidateAccessToken;

    @BindView(R.id.debug_invalidateRefreshToken) View invalidateRefreshToken;

    @BindView(R.id.debug_insertFakeShow) View insertFakeShow;

    @BindView(R.id.debug_removeFakeShow) View removeFakeShow;

    @BindView(R.id.debug_logLevel) Spinner logLevel;

    @BindView(R.id.debug_networkStatusCode) Spinner httpStatusCode;

    @BindView(R.id.debug_drawer) ViewGroup drawerContent;

    @BindView(R.id.debug_syncConfiguration) View syncConfiguration;

    @BindView(R.id.debug_initialSync) View initialSync;

    @BindView(R.id.debug_forceUpdate) View forceUpdate;

    @BindView(R.id.debug_updated) View updated;

    @BindView(R.id.debug_syncWatching) View syncWatching;

    @BindView(R.id.debug_syncLists) View syncLists;

    @BindView(R.id.debug_authHandlerStatus) TextView authHandlerStatus;

    @BindView(R.id.debug_dataHandlerStatus) TextView dataHandlerStatus;

    @BindView(R.id.debug_jobCount) TextView jobCount;

    @BindView(R.id.debug_startPeriodicJobs) View startPeriodicJobs;
  }
}
