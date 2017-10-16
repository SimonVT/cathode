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

package net.simonvt.cathode.sync.jobqueue;

import net.simonvt.cathode.common.event.SyncEvent;
import net.simonvt.cathode.remote.Flags;

public class DataJobHandler extends JobHandler {

  private static final int THREAD_COUNT = 3;

  private static volatile DataJobHandler instance = new DataJobHandler();

  public static DataJobHandler getInstance() {
    return instance;
  }

  public DataJobHandler() {
    super(0, Flags.REQUIRES_AUTH, THREAD_COUNT);
  }

  @Override protected void onResume() {
    SyncEvent.dataExecutorStarted();
  }

  @Override protected void onPause() {
    SyncEvent.dataExecutorStopped();
  }
}
