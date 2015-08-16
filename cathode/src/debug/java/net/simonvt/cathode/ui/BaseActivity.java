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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.HttpStatusCode;
import net.simonvt.cathode.IntPreference;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.RequestFailedEvent;
import net.simonvt.cathode.jobqueue.AuthJobService;
import net.simonvt.cathode.jobqueue.AuthSyncEvent;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobListener;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.jobqueue.JobService;
import net.simonvt.cathode.jobqueue.SyncEvent;
import net.simonvt.cathode.remote.ForceUpdateJob;
import net.simonvt.cathode.remote.InitialSyncJob;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.lists.SyncLists;
import net.simonvt.cathode.remote.sync.movies.StartSyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.shows.StartSyncUpdatedShows;
import net.simonvt.cathode.settings.Settings;
import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity {

  private final DebugViews debugViews = new DebugViews();

  private final DebugInjects injects = new DebugInjects();

  private final DebugSubscribers subscribers = new DebugSubscribers();

  private SharedPreferences settings;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(this);
    CathodeApp.inject(this, injects);

    super.setContentView(R.layout.debug_home);

    settings = PreferenceManager.getDefaultSharedPreferences(this);

    Context drawerContext = new ContextThemeWrapper(this, R.style.Theme_AppCompat);
    LayoutInflater.from(drawerContext)
        .inflate(R.layout.debug_drawer, (ViewGroup) ButterKnife.findById(this, R.id.debug_drawer));

    ButterKnife.bind(debugViews, this);

    injects.bus.register(subscribers);

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
    StartPage startPage = StartPage.fromValue(settings.getString(Settings.START_PAGE, null),
        StartPage.SHOWS_UPCOMING);
    debugViews.startPage.setSelection(startPageAdapter.getPositionForValue(startPage));

    debugViews.requestFailedEvent.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.bus.post(new RequestFailedEvent(R.string.error_unknown_retrying));
        debugViews.drawerLayout.closeDrawers();
      }
    });

    debugViews.authFailedEvent.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.bus.post(new AuthFailedEvent());
        debugViews.drawerLayout.closeDrawers();
      }
    });

    int[] statusCodes = new int[] {
        200, 401, 404, 412, 502,
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

    debugViews.updatedLastDay.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        String lastUpdated = TimeUtils.getIsoTime();
        settings.edit()
            .putString(Settings.MOVIES_LAST_UPDATED, lastUpdated)
            .putString(Settings.SHOWS_LAST_UPDATED, lastUpdated)
            .apply();
        injects.jobManager.addJob(new StartSyncUpdatedShows());
        injects.jobManager.addJob(new StartSyncUpdatedMovies());
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

    debugViews.startJobService.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intent i = new Intent(BaseActivity.this, JobService.class);
        startService(i);

        i = new Intent(BaseActivity.this, AuthJobService.class);
        startService(i);
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
    injects.bus.unregister(subscribers);
    super.onDestroy();
  }

  private class DebugSubscribers {

    @Subscribe public void onJobSyncEvent(SyncEvent event) {
      Timber.d("JobService: " + event.isSyncing());
      if (event.isSyncing()) {
        debugViews.jobServiceStatus.setText("Running");
      } else {
        debugViews.jobServiceStatus.setText("Stopped");
      }
    }

    @Subscribe public void onAuthJobSyncEvent(AuthSyncEvent event) {
      Timber.d("AuthJobService: " + event.isSyncing());
      if (event.isSyncing()) {
        debugViews.authJobServiceStatus.setText("Running");
      } else {
        debugViews.authJobServiceStatus.setText("Stopped");
      }
    }
  }

  public static class DebugInjects {

    @Inject Bus bus;

    @Inject @HttpStatusCode IntPreference httpStatusCodePreference;

    @Inject JobManager jobManager;
  }

  static class DebugViews {

    @Bind(R.id.debug_drawerLayout) DrawerLayout drawerLayout;

    @Bind(R.id.debug_content) ViewGroup container;

    @Bind(R.id.debug_startPage) Spinner startPage;

    @Bind(R.id.debug_requestFailedEvent) View requestFailedEvent;

    @Bind(R.id.debug_authFailedEvent) View authFailedEvent;

    @Bind(R.id.debug_networkStatusCode) Spinner httpStatusCode;

    @Bind(R.id.debug_drawer) ViewGroup drawerContent;

    @Bind(R.id.debug_initialSync) View initialSync;

    @Bind(R.id.debug_forceUpdate) View forceUpdate;

    @Bind(R.id.debug_updatedLastDay) View updatedLastDay;

    @Bind(R.id.debug_syncWatching) View syncWatching;

    @Bind(R.id.debug_syncLists) View syncLists;

    @Bind(R.id.debug_authJobServiceStatus) TextView authJobServiceStatus;

    @Bind(R.id.debug_jobServiceStatus) TextView jobServiceStatus;

    @Bind(R.id.debug_jobCount) TextView jobCount;

    @Bind(R.id.debug_startJobService) TextView startJobService;
  }
}
