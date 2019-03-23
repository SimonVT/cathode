/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
import net.simonvt.cathode.actions.ActionFailedException
import net.simonvt.cathode.actions.ErrorHandlerAction
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.SeasonService
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.event.ItemsUpdatedEvent
import net.simonvt.cathode.common.http.requireBody
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.delete
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.update
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class SyncPendingSeasons @Inject constructor(
  private val context: Context,
  private val seasonService: SeasonService,
  private val showHelper: ShowDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper
) : ErrorHandlerAction<Unit>() {

  override suspend fun invoke(params: Unit) {
    val seasons = context.contentResolver.query(
      Seasons.SEASONS,
      arrayOf(SeasonColumns.ID, SeasonColumns.SHOW_ID, SeasonColumns.SEASON),
      SeasonColumns.NEEDS_SYNC
    )
    try {
      seasons.forEach {
        val seasonId = Cursors.getLong(seasons, SeasonColumns.ID)
        val showId = Cursors.getLong(seasons, SeasonColumns.SHOW_ID)
        val showTraktId = showHelper.getTraktId(showId)
        val season = Cursors.getInt(seasons, SeasonColumns.SEASON)

        Timber.d("Syncing pending season %d-%d", showTraktId, season)

        val call = seasonService.getSeason(showTraktId, season, Extended.FULL)
        val response = call.execute()
        if (response.isSuccessful) {
          val episodeIds = mutableListOf<Long>()
          val episodes = response.requireBody()

          val episodesCursor = context.contentResolver.query(
            Episodes.fromSeason(seasonId),
            arrayOf(EpisodeColumns.ID)
          )
          episodesCursor.forEach { cursor ->
            val episodeId = cursor.getLong(EpisodeColumns.ID)
            episodeIds.add(episodeId)
          }
          episodesCursor.close()

          episodes.forEach { episode ->
            val episodeResult = episodeHelper.getIdOrCreate(showId, seasonId, episode.number!!)
            val episodeId = episodeResult.id
            episodeHelper.updateEpisode(episodeId, episode)
            episodeIds.remove(episodeId)
          }

          episodeIds.forEach { episodeId -> context.contentResolver.delete(Episodes.withId(episodeId)) }

          val values = ContentValues()
          values.put(SeasonColumns.NEEDS_SYNC, false)
          context.contentResolver.update(Seasons.withId(seasonId), values)
        } else if (isError(response)) {
          throw ActionFailedException()
        }

        if (stopped) {
          return
        }
      }

      ItemsUpdatedEvent.post()
    } catch (e: IOException) {
      Timber.d(e)
      throw ActionFailedException(e)
    } finally {
      seasons.close()
    }
  }
}
