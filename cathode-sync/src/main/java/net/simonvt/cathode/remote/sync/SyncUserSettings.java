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
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.settings.ProfileSettings;
import retrofit2.Call;

public class SyncUserSettings extends CallJob<UserSettings> {

  @Inject transient UsersService usersService;

  public SyncUserSettings() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncUserSettings";
  }

  @Override public int getPriority() {
    return JobPriority.USER_DATA;
  }

  @Override public Call<UserSettings> getCall() {
    return usersService.getUserSettings();
  }

  @Override public boolean handleResponse(UserSettings userSettings) {
    ProfileSettings.updateProfile(getContext(), userSettings);
    return true;
  }
}
