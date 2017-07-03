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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

public final class Jobs {

  private Jobs() {
  }

  public static boolean usesScheduler() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  public static void schedule(Context context, JobInfo job) {
    JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    scheduler.schedule(job);
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  public static void scheduleNotPending(Context context, JobInfo job) {
    JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (scheduler.getPendingJob(job.getId()) == null) {
      scheduler.schedule(job);
    }
  }
}
