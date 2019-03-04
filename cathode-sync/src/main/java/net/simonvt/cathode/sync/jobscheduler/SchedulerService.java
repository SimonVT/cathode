/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.sync.jobscheduler;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.SparseArray;
import com.crashlytics.android.Crashlytics;
import dagger.android.AndroidInjection;
import dagger.android.DispatchingAndroidInjector;
import io.fabric.sdk.android.Fabric;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import net.simonvt.cathode.jobqueue.Job;
import timber.log.Timber;

public class SchedulerService extends JobService {

  private static final int THREAD_COUNT = 3;

  @Inject DispatchingAndroidInjector<Job> jobInjector;
  @Inject JobCreator jobCreator;

  private ExecutorService executor;
  private SparseArray<Job> runningJobs = new SparseArray<>();

  @Override public void onCreate() {
    super.onCreate();
    // TODO: Stop if not app context
    AndroidInjection.inject(this);
    executor = Executors.newFixedThreadPool(THREAD_COUNT);
  }

  @Override public void onDestroy() {
    executor.shutdown();
    super.onDestroy();
  }

  @Override public boolean onStartJob(final JobParameters params) {
    Timber.d("[onStartJob] %d", params.getJobId());

    // There's a bug in Android N where backup can leave the process in an unusable state. When that
    // happens, have JobScheduler reschedule the job.
    if (jobCreator == null) {
      if (!Fabric.isInitialized()) {
        Fabric.with(this, new Crashlytics());
      }
      Crashlytics.logException(new RuntimeException("Process in unusable state, rescheduling"));
      jobFinished(params, true);
      return false;
    }

    switch (params.getJobId()) {
      case AuthJobHandlerJob.ID:
      case AuthJobHandlerJob.ID_ONESHOT: {
        Job job = new AuthJobHandlerJob(this, params);
        jobInjector.inject(job);
        runningJobs.put(params.getJobId(), job);
        job.perform();
        return true;
      }

      case DataJobHandlerJob.ID: {
        Job job = new DataJobHandlerJob(this, params);
        jobInjector.inject(job);
        runningJobs.put(params.getJobId(), job);
        job.perform();
        return true;
      }

      default: {
        Job job = jobCreator.create(params);
        runningJobs.put(params.getJobId(), job);
        execute(params, job);
        return true;
      }
    }
  }

  private void execute(final JobParameters params, final Job job) {
    executor.execute(new Runnable() {
      @Override public void run() {
        try {
          if (job.perform()) {
            Timber.d("[jobFinished](success) %d", params.getJobId());
            jobFinished(params, false);
          } else {
            Timber.d("[jobFinished](failure) %d", params.getJobId());
            jobFinished(params, true);
          }
        } catch (Throwable t) {
          Timber.d(t, "Job failed: %s", job.getClass().getCanonicalName());
          jobFinished(params, true);
        } finally {
          runningJobs.remove(params.getJobId());
        }
      }
    });
  }

  @Override public boolean onStopJob(JobParameters params) {
    Timber.d("[onStopJob] %d", params.getJobId());
    Job job = runningJobs.get(params.getJobId());
    if (job != null) {
      job.stop();
      runningJobs.remove(params.getJobId());
    }
    return true;
  }
}
