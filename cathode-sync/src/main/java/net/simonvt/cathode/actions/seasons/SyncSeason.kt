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
package net.simonvt.cathode.actions.seasons

import android.content.ContentValues
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.seasons.SyncSeason.Params
import net.simonvt.cathode.api.entity.Episode
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.SeasonService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.delete
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.update
import retrofit2.Call
import javax.inject.Inject

class SyncSeason @Inject constructor(
  private val context: Context,
  private val seasonService: SeasonService,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper
) : CallAction<Params, List<Episode>>() {

  override fun getCall(params: Params): Call<List<Episode>> =
    seasonService.getSeason(params.traktId, params.season, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: List<Episode>) {
    val showId = showHelper.getId(params.traktId)
    val seasonResult = seasonHelper.getIdOrCreate(showId, params.season)
    val seasonId = seasonResult.id

    val episodeIds = mutableListOf<Long>()
    val currentEpisodes = context.contentResolver.query(
      Episodes.fromSeason(seasonId),
      arrayOf(EpisodeColumns.ID)
    )
    currentEpisodes.forEach { cursor -> episodeIds.add(cursor.getLong(EpisodeColumns.ID)) }
    currentEpisodes.close()

    for (episode in response) {
      val episodeResult = episodeHelper.getIdOrCreate(showId, seasonId, episode.number!!)
      val episodeId = episodeResult.id
      episodeHelper.updateEpisode(episodeId, episode)
      episodeIds.remove(episodeId)
    }

    for (episodeId in episodeIds) {
      context.contentResolver.delete(Episodes.withId(episodeId))
    }

    val values = ContentValues()
    values.put(SeasonColumns.NEEDS_SYNC, false)
    context.contentResolver.update(Seasons.withId(seasonId), values)
  }

  data class Params(val traktId: Long, val season: Int)

  companion object {

    fun key(traktId: Long, season: Int) = "SyncSeason&traktId=$traktId&season=$season"
  }
}
