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

package net.simonvt.cathode.actions.seasons

import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.seasons.SyncSeasons.Params
import net.simonvt.cathode.api.entity.Season
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.SeasonService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.delete
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import javax.inject.Inject

class SyncSeasons @Inject constructor(
  private val context: Context,
  private val seasonService: SeasonService,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val syncSeason: SyncSeason
) : CallAction<Params, List<Season>>() {

  override fun key(params: Params): String = "SyncSeasons&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<List<Season>> =
    seasonService.getSummary(params.traktId, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: List<Season>) {
    val showId = showHelper.getId(params.traktId)
    val seasonIds = mutableListOf<Long>()
    val currentSeasons =
      context.contentResolver.query(Seasons.fromShow(showId), arrayOf(SeasonColumns.ID))
    currentSeasons.forEach { cursor -> seasonIds.add(cursor.getLong(SeasonColumns.ID)) }
    currentSeasons.close()

    for (season in response) {
      val result = seasonHelper.getIdOrCreate(showId, season.number)
      seasonHelper.updateSeason(showId, season)
      seasonIds.remove(result.id)
      syncSeason(SyncSeason.Params(params.traktId, season.number))
    }

    for (seasonId in seasonIds) {
      context.contentResolver.delete(Seasons.withId(seasonId))
    }
  }

  data class Params(val traktId: Long)
}
