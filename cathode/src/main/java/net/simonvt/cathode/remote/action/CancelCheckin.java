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

package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;

public class CancelCheckin extends Job {

  @Inject transient CheckinService checkinService;

  public CancelCheckin() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "CancelCheckin";
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public void perform() {
    checkinService.deleteCheckin();
  }
}
