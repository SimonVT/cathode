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

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.user.SyncEpisodesRatings.Params
import net.simonvt.cathode.api.entity.RatingItem
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncEpisodesRatings @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val syncService: SyncService
) : CallAction<Params, List<RatingItem>>() {

  override fun key(params: Params): String = "SyncEpisodesRatings"

  override fun getCall(params: Params): Call<List<RatingItem>> = syncService.getEpisodeRatings()

  override suspend fun handleResponse(params: Params, response: List<RatingItem>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val episodeIds = mutableListOf<Long>()

    val episodes = context.contentResolver.query(
      Episodes.EPISODES,
      arrayOf(EpisodeColumns.ID),
      EpisodeColumns.RATED_AT + ">0"
    )
    episodes.forEach { cursor -> episodeIds.add(cursor.getLong(EpisodeColumns.ID)) }
    episodes.close()

    for (rating in response) {
      val seasonNumber = rating.episode!!.season!!
      val episodeNumber = rating.episode!!.number!!

      val showTraktId = rating.show!!.ids.trakt!!
      val showResult = showHelper.getIdOrCreate(showTraktId)
      val showId = showResult.showId

      val seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber)
      val seasonId = seasonResult.id

      val episodeResult = episodeHelper.getIdOrCreate(showId, seasonId, episodeNumber)
      val episodeId = episodeResult.id
      episodeIds.remove(episodeId)

      val op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
        .withValue(EpisodeColumns.USER_RATING, rating.rating)
        .withValue(EpisodeColumns.RATED_AT, rating.rated_at.timeInMillis)
        .build()
      ops.add(op)
    }

    for (episodeId in episodeIds) {
      val op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
        .withValue(EpisodeColumns.USER_RATING, 0)
        .withValue(EpisodeColumns.RATED_AT, 0)
        .build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.EPISODE_RATING, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)
}
