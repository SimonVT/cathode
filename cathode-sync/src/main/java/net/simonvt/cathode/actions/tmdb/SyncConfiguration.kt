/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.actions.tmdb

import android.content.Context
import com.uwetrottmann.tmdb2.entities.Configuration
import com.uwetrottmann.tmdb2.services.ConfigurationService
import net.simonvt.cathode.actions.TmdbCallAction
import net.simonvt.cathode.images.ImageSettings
import retrofit2.Call
import javax.inject.Inject

class SyncConfiguration @Inject constructor(
  private val context: Context,
  private val configurationService: ConfigurationService
) : TmdbCallAction<Unit, Configuration>() {

  override fun key(params: Unit): String = "SyncConfiguration"

  override fun getCall(params: Unit): Call<Configuration> = configurationService.configuration()

  override suspend fun handleResponse(params: Unit, response: Configuration) {
    ImageSettings.updateTmdbConfiguration(context, response)
  }
}
