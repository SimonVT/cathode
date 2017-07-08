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
import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;
import net.simonvt.cathode.common.util.MainHandler;
import net.simonvt.cathode.jobqueue.DataJobHandler;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobHandler;
import timber.log.Timber;

@RequiresApi(api = Build.VERSION_CODES.N) public class DataJobHandlerJob extends Job {

  public static final int ID = 2;

  private DataJobHandler jobHandler;

  private SchedulerService service;
  private JobParameters params;

  public static void schedulePeriodic(Context context) {
    JobInfo jobInfo = new JobInfo.Builder(ID, new ComponentName(context, SchedulerService.class)) //
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setRequiresCharging(true)
        .setRequiresDeviceIdle(true)
        .setPeriodic(DateUtils.DAY_IN_MILLIS)
        .setPersisted(true)
        .build();
    Jobs.schedule(context, jobInfo);
  }

  public DataJobHandlerJob(SchedulerService service, JobParameters params) {
    this.service = service;
    this.params = params;
    jobHandler = DataJobHandler.getInstance();
  }

  @Override public String key() {
    throw new RuntimeException("Must not be added to JobManager");
  }

  @Override public int getPriority() {
    throw new RuntimeException("Must not be added to JobManager");
  }

  public boolean perform() {
    if (jobHandler.hasJobs()) {
      jobHandler.registerListener(listener);
    } else {
      // https://issuetracker.google.com/issues/37018640
      MainHandler.post(new Runnable() {
        @Override public void run() {
          Timber.d("[jobFinished][success] %d", params.getJobId());
          service.jobFinished(params, false);
        }
      });
    }

    return true;
  }

  @Override public void stop() {
    super.stop();
    jobHandler.unregisterListener(listener);
  }

  private JobHandler.JobHandlerListener listener = new JobHandler.JobHandlerListener() {
    @Override public void onQueueEmpty() {
      Timber.d("[jobFinished][success] %d", params.getJobId());
      service.jobFinished(params, false);
      MainHandler.post(new Runnable() {
        @Override public void run() {
          jobHandler.unregisterListener(listener);
        }
      });
    }

    @Override public void onQueueFailed() {
      Timber.d("[jobFinished][failure] %d", params.getJobId());
      service.jobFinished(params, true);
      MainHandler.post(new Runnable() {
        @Override public void run() {
          jobHandler.unregisterListener(listener);
        }
      });
    }
  };
}
