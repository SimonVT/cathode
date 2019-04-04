/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.actions.shows

import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.seasons.SyncSeasons
import net.simonvt.cathode.actions.shows.SyncShow.Params
import net.simonvt.cathode.api.entity.Show
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.ShowsService
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import retrofit2.Call
import javax.inject.Inject

class SyncShow @Inject constructor(
  private val showsService: ShowsService,
  private val showHelper: ShowDatabaseHelper,
  private val syncSeasons: SyncSeasons
) : CallAction<Params, Show>() {

  override fun key(params: Params): String = "SyncShow&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<Show> =
    showsService.getSummary(params.traktId, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: Show) {
    syncSeasons(SyncSeasons.Params(params.traktId))
    showHelper.fullUpdate(response)
  }

  data class Params(val traktId: Long)
}
