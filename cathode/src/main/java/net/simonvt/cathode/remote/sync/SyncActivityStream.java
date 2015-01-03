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

import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.settings.TraktTimestamps;

public class SyncActivityStream extends Job {

  public SyncActivityStream() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncActivityStream";
  }

  @Override public int getPriority() {
    return PRIORITY_4;
  }

  @Override public void perform() {
    final long lastSync = TraktTimestamps.lastActivityStreamSync(getContext());

    // TODO: Recent actions by user

    queue(new SyncWatching());
  }
}
