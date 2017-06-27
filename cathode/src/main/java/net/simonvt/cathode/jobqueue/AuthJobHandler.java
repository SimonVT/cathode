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

package net.simonvt.cathode.jobqueue;

import android.content.Intent;
import net.simonvt.cathode.event.SyncEvent;
import net.simonvt.cathode.jobscheduler.AuthJobHandlerJob;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.remote.Flags;

public class AuthJobHandler extends JobHandler {

  private static final int THREAD_COUNT = 1;

  private static volatile AuthJobHandler instance = new AuthJobHandler();

  public static AuthJobHandler getInstance() {
    return instance;
  }

  public AuthJobHandler() {
    super(Flags.REQUIRES_AUTH, 0, THREAD_COUNT);
  }

  @Override protected void onResume() {
    SyncEvent.authExecutorStarted();
  }

  @Override protected void onPause() {
    SyncEvent.authExecutorStopped();
  }

  @Override protected void onStop() {
    if (hasJobs()) {
      if (Jobs.usesScheduler()) {
        AuthJobHandlerJob.schedule(context);
      } else {
        Intent i = new Intent(context, AuthJobService.class);
        context.startService(i);
      }
    }
  }
}
