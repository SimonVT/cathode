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
package net.simonvt.cathode.actions.user

import android.content.Context
import androidx.work.WorkManager
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.user.SyncEpisodeWatchlist.Params
import net.simonvt.cathode.api.entity.WatchlistItem
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseSchematic
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import retrofit2.Call
import javax.inject.Inject

class SyncEpisodeWatchlist @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val syncService: SyncService,
  private val workManager: WorkManager
) : CallAction<Params, List<WatchlistItem>>() {

  override fun key(params: Params): String = "SyncEpisodeWatchlist"

  override fun getCall(params: Params): Call<List<WatchlistItem>> =
    syncService.getEpisodeWatchlist()

  override suspend fun handleResponse(params: Params, response: List<WatchlistItem>) {
    val episodeIds = mutableListOf<Long>()
    val localWatchlist = context.contentResolver.query(
      Episodes.EPISODES_IN_WATCHLIST,
      arrayOf(DatabaseSchematic.Tables.EPISODES + "." + EpisodeColumns.ID)
    )
    localWatchlist.forEach { cursor -> episodeIds.add(cursor.getLong(EpisodeColumns.ID)) }
    localWatchlist.close()

    for (watchlistItem in response) {
      val showTraktId = watchlistItem.show!!.ids.trakt!!
      val seasonNumber = watchlistItem.episode!!.season!!
      val episodeNumber = watchlistItem.episode!!.number!!
      val listedAt = watchlistItem.listed_at.timeInMillis

      val showResult = showHelper.getIdOrCreate(showTraktId)
      val showId = showResult.showId
      val didShowExist = !showResult.didCreate
      if (showResult.didCreate) {
        showHelper.partialUpdate(watchlistItem.show!!)
      }

      val seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber)
      val seasonId = seasonResult.id
      val didSeasonExist = !seasonResult.didCreate
      if (seasonResult.didCreate && didShowExist) {
        showHelper.markPending(showId)
      }

      val episodeResult = episodeHelper.getIdOrCreate(showId, seasonId, episodeNumber)
      val episodeId = episodeResult.id
      if (episodeResult.didCreate && didShowExist && didSeasonExist) {
        showHelper.markPending(showId)
      }

      if (!episodeIds.remove(episodeId)) {
        episodeHelper.setIsInWatchlist(episodeId, true, listedAt)
      }
    }

    for (episodeId in episodeIds) {
      episodeHelper.setIsInWatchlist(episodeId, false, 0L)
    }

    workManager.enqueueUniqueNow(SyncPendingShowsWorker.TAG, SyncPendingShowsWorker::class.java)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.EPISODE_WATCHLIST, params.userActivityTime)
        .apply()
    }
  }

  data class Params(val userActivityTime: Long = 0L)
}
