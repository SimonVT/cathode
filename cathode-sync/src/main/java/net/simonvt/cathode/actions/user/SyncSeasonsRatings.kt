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
import net.simonvt.cathode.actions.user.SyncSeasonsRatings.Params
import net.simonvt.cathode.api.entity.RatingItem
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncSeasonsRatings @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val syncService: SyncService
) : CallAction<Params, List<RatingItem>>() {

  override fun key(params: Params): String = "SyncSeasonsRatings"

  override fun getCall(params: Params): Call<List<RatingItem>> = syncService.getSeasonRatings()

  override suspend fun handleResponse(params: Params, response: List<RatingItem>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val seasonIds = mutableListOf<Long>()

    val seasons = context.contentResolver.query(
      Seasons.SEASONS,
      arrayOf(SeasonColumns.ID),
      SeasonColumns.RATED_AT + ">0"
    )
    seasons.forEach { cursor -> seasonIds.add(cursor.getLong(SeasonColumns.ID)) }
    seasons.close()

    for (rating in response) {
      val seasonNumber = rating.season!!.number

      val showTraktId = rating.show!!.ids.trakt!!
      val showResult = showHelper.getIdOrCreate(showTraktId)
      val showId = showResult.showId

      val seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber)
      val seasonId = seasonResult.id
      seasonIds.remove(seasonId)

      val op = ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
        .withValue(SeasonColumns.USER_RATING, rating.rating)
        .withValue(SeasonColumns.RATED_AT, rating.rated_at.timeInMillis)
        .build()
      ops.add(op)
    }

    for (seasonId in seasonIds) {
      val op = ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
        .withValue(SeasonColumns.USER_RATING, 0)
        .withValue(SeasonColumns.RATED_AT, 0)
        .build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.SEASON_RATING, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)
}
