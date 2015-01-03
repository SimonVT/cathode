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

import javax.inject.Inject;
import net.simonvt.cathode.api.entity.UserSettings;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.settings.Settings;

public class SyncUserSettings extends Job {

  @Inject transient UsersService usersService;

  public SyncUserSettings() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncUserSettings";
  }

  @Override public int getPriority() {
    return PRIORITY_5;
  }

  @Override public void perform() {
    UserSettings userSettings = usersService.getUserSettings();
    Settings.updateProfile(getContext(), userSettings);
  }
}
