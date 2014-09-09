/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote.sync;

import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.TraktTimestamps;

public class SyncActivityStreamTask extends TraktTask {

  @Override protected void doTask() {
    final long lastSync = TraktTimestamps.lastActivityStreamSync(getContext());

    // TODO: Recent actions by user

    queueTask(new SyncWatchingTask());
    postOnSuccess();
  }
}
