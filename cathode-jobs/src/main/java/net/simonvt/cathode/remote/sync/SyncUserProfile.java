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

package net.simonvt.cathode.remote.sync;

import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Profile;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.provider.UserDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class SyncUserProfile extends CallJob<Profile> {

  @Inject transient UsersService usersService;

  @Inject transient UserDatabaseHelper userHelper;

  public SyncUserProfile() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncUserProfile";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public Call<Profile> getCall() {
    return usersService.getProfile(Extended.FULL_IMAGES);
  }

  @Override public boolean handleResponse(Profile profile) {
    userHelper.updateOrCreate(profile);
    return true;
  }
}
