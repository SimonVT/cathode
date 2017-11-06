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
package net.simonvt.cathode.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import dagger.android.DispatchingAndroidInjector;
import java.util.concurrent.CountDownLatch;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.sync.jobqueue.AuthJobHandler;
import net.simonvt.cathode.sync.jobqueue.DataJobHandler;
import net.simonvt.cathode.sync.jobqueue.JobHandler;
import timber.log.Timber;

public class CathodeSyncAdapter extends AbstractThreadedSyncAdapter {

  AuthJobHandler authJobHandler;
  DataJobHandler dataJobHandler;
  JobManager jobManager;
  DispatchingAndroidInjector<Job> jobInjector;

  public CathodeSyncAdapter(Context context, AuthJobHandler authJobHandler,
      DataJobHandler dataJobHandler, JobManager jobManager,
      DispatchingAndroidInjector<Job> jobInjector) {
    super(context, true);
    this.authJobHandler = authJobHandler;
    this.dataJobHandler = dataJobHandler;
    this.jobManager = jobManager;
    this.jobInjector = jobInjector;
  }

  @Override public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    if (jobManager == null) {
      return;
    }

    if (!TraktLinkSettings.isLinked(getContext())) {
      return;
    }

    if (inject(new SyncJob()).perform()) {
      final CountDownLatch latch = new CountDownLatch(2);

      if (authJobHandler.hasJobs()) {
        authJobHandler.registerListener(new JobHandler.JobHandlerListener() {

          @Override public void onQueueEmpty() {
            latch.countDown();
            authJobHandler.unregisterListener(this);
          }

          @Override public void onQueueFailed() {
            latch.countDown();
            authJobHandler.unregisterListener(this);
          }
        });
      } else {
        latch.countDown();
      }

      if (dataJobHandler.hasJobs()) {
        dataJobHandler.registerListener(new JobHandler.JobHandlerListener() {

          @Override public void onQueueEmpty() {
            latch.countDown();
            dataJobHandler.unregisterListener(this);
          }

          @Override public void onQueueFailed() {
            latch.countDown();
            dataJobHandler.unregisterListener(this);
          }
        });
      } else {
        latch.countDown();
      }

      try {
        latch.await();
      } catch (InterruptedException e) {
        Timber.d(e);
      }
    } else {
      jobManager.addJob(new SyncJob());
    }
  }

  private Job inject(Job job) {
    jobInjector.inject(job);
    return job;
  }
}
