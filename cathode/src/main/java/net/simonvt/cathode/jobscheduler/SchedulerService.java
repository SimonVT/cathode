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

package net.simonvt.cathode.jobscheduler;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.SparseArray;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.simonvt.cathode.jobqueue.Job;
import timber.log.Timber;

@RequiresApi(api = Build.VERSION_CODES.N) public class SchedulerService extends JobService {

  private static final int THREAD_COUNT = 3;

  private JobCreator jobCreator;
  private ExecutorService executor;
  private SparseArray<Job> runningJobs = new SparseArray<>();

  @Override public void onCreate() {
    super.onCreate();
    jobCreator = new JobCreator();
    executor = Executors.newFixedThreadPool(THREAD_COUNT);
  }

  @Override public void onDestroy() {
    executor.shutdown();
    super.onDestroy();
  }

  @Override public boolean onStartJob(final JobParameters params) {
    Timber.d("[onStartJob] %d", params.getJobId());

    if (params.getJobId() == AuthJobHandlerJob.ID) {
      Job job = new AuthJobHandlerJob(this, params);
      runningJobs.put(params.getJobId(), job);
      job.perform();
      return true;
    }
    if (params.getJobId() == DataJobHandlerJob.ID) {
      Job job = new DataJobHandlerJob(this, params);
      runningJobs.put(params.getJobId(), job);
      job.perform();
      return true;
    }

    Job job = jobCreator.create(params);
    runningJobs.put(params.getJobId(), job);
    execute(params, job);
    return true;
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
