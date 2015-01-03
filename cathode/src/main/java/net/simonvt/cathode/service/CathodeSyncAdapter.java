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
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.jobqueue.JobManager;

public class CathodeSyncAdapter extends AbstractThreadedSyncAdapter {

  @Inject JobManager jobManager;

  public CathodeSyncAdapter(Context context) {
    super(context, true);
    CathodeApp.inject(context, this);
  }

  @Override public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    if (Settings.isLoggedIn(getContext())) {
      jobManager.addJob(new SyncJob());
    }
  }
}
