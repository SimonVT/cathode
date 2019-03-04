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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.List;

public final class Jobs {

  private Jobs() {
  }

  public static JobScheduler getScheduler(Context context) {
    return (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
  }

  public static void schedule(Context context, JobInfo job) {
    JobScheduler scheduler = getScheduler(context);
    scheduler.schedule(job);
  }

  public static void scheduleNotPending(Context context, JobInfo job) {
    JobScheduler scheduler = getScheduler(context);
    if (getPendingJob(context, job.getId()) == null) {
      scheduler.schedule(job);
    }
  }

  public static void cancel(Context context, int jobId) {
    JobScheduler scheduler = getScheduler(context);
    scheduler.cancel(jobId);
  }

  public static JobInfo getPendingJob(Context context, int jobId) {
    JobScheduler scheduler = getScheduler(context);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return scheduler.getPendingJob(jobId);
    } else {
      List<JobInfo> pendingJobs = scheduler.getAllPendingJobs();
      for (JobInfo job : pendingJobs) {
        if (jobId == job.getId()) {
          return job;
        }
      }

      return null;
    }
  }
}
