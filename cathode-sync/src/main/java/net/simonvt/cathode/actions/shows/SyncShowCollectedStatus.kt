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

package net.simonvt.cathode.actions.shows

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.shows.SyncShowCollectedStatus.Params
import net.simonvt.cathode.api.entity.ShowProgress
import net.simonvt.cathode.api.service.ShowsService
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import retrofit2.Call
import javax.inject.Inject

class SyncShowCollectedStatus @Inject constructor(
  private val context: Context,
  private val showsService: ShowsService,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper
) : CallAction<Params, ShowProgress>() {

  override fun key(params: Params): String = "SyncShowCollectedStatus&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<ShowProgress> =
    showsService.getCollectionProgress(params.traktId)

  override suspend fun handleResponse(params: Params, response: ShowProgress) {
    val showResult = showHelper.getIdOrCreate(params.traktId)
    val showId = showResult.showId

    val ops = arrayListOf<ContentProviderOperation>()

    for (season in response.seasons) {
      val seasonResult = seasonHelper.getIdOrCreate(showId, season.number)
      val seasonId = seasonResult.id

      for (episode in season.episodes) {
        val episodeResult = episodeHelper.getIdOrCreate(showId, seasonId, episode.number)
        val episodeId = episodeResult.id

        val builder = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
          .withValue(EpisodeColumns.IN_COLLECTION, episode.completed)
          .withValue(EpisodeColumns.COLLECTED_AT, episode.collected_at?.timeInMillis)
        ops.add(builder.build())
      }
    }

    context.contentResolver.batch(ops)
  }

  data class Params(val traktId: Long)
}
