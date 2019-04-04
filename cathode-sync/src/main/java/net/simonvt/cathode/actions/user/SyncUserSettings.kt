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

package net.simonvt.cathode.actions.user

import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.api.entity.UserSettings
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.settings.ProfileSettings
import retrofit2.Call
import javax.inject.Inject

class SyncUserSettings @Inject constructor(
  private val context: Context,
  private val usersService: UsersService
) : CallAction<Unit, UserSettings>() {

  override fun key(params: Unit): String = "SyncUserSettings"

  override fun getCall(params: Unit): Call<UserSettings> = usersService.getUserSettings()

  override suspend fun handleResponse(params: Unit, response: UserSettings) {
    ProfileSettings.updateProfile(context, response)
  }
}
