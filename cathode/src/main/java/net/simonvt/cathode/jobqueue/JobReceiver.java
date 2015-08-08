/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.jobqueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import timber.log.Timber;

public class JobReceiver extends BroadcastReceiver {

  private static final String TAG = "JobReceiver";

  @Override public void onReceive(Context context, Intent intent) {
    Timber.tag(TAG).i("Intent: %s", intent.toString());
    JobService.acquireLock(context);

    final int retryDelay = intent.getIntExtra(JobService.RETRY_DELAY, 1);

    Intent i = new Intent(context, JobService.class);
    i.putExtra(JobService.RETRY_DELAY, retryDelay);
    context.startService(i);
  }
}
