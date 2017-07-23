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
package net.simonvt.cathode.service;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import java.util.concurrent.CountDownLatch;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.jobqueue.AuthJobHandler;
import net.simonvt.cathode.jobqueue.DataJobHandler;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobHandler;
import net.simonvt.cathode.jobqueue.JobInjector;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.settings.TraktLinkSettings;
import timber.log.Timber;

public class CathodeSyncAdapter extends AbstractThreadedSyncAdapter {

  @Inject JobManager jobManager;
  @Inject JobInjector injector;

  public CathodeSyncAdapter(Context context) {
    super(context, true);
    // This might not be true when Android restores backup on install.
    if (context.getApplicationContext() instanceof CathodeApp) {
      Injector.obtain().inject(this);
    }
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

      final AuthJobHandler authJobHandler = AuthJobHandler.getInstance();
      final DataJobHandler dataJobHandler = DataJobHandler.getInstance();

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
    injector.injectInto(job);
    return job;
  }
}
