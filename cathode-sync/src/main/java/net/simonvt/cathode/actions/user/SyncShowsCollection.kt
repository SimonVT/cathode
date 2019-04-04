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

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import androidx.work.WorkManager
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.user.SyncShowsCollection.Params
import net.simonvt.cathode.api.entity.CollectionItem
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.TraktTimestamps
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import retrofit2.Call
import java.util.ArrayList
import javax.inject.Inject

class SyncShowsCollection @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val syncService: SyncService,
  private val workManager: WorkManager
) : CallAction<Params, List<CollectionItem>>() {

  override fun key(params: Params): String = "SyncShowsCollection"

  override fun getCall(params: Params): Call<List<CollectionItem>> = syncService.getShowCollection()

  override suspend fun handleResponse(params: Params, response: List<CollectionItem>) {
    val c = context.contentResolver.query(
      Episodes.EPISODES,
      arrayOf(
        EpisodeColumns.ID,
        EpisodeColumns.SHOW_ID,
        EpisodeColumns.SEASON,
        EpisodeColumns.SEASON_ID,
        EpisodeColumns.EPISODE,
        EpisodeColumns.COLLECTED_AT
      ),
      EpisodeColumns.IN_COLLECTION
    )

    val showsMap = mutableMapOf<Long, CollectedShow>()
    val showIdToTraktMap = mutableMapOf<Long, Long>()
    val episodeIds = mutableListOf<Long>()

    while (c.moveToNext()) {
      val id = Cursors.getLong(c, EpisodeColumns.ID)
      val showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID)
      val season = Cursors.getInt(c, EpisodeColumns.SEASON)
      val seasonId = Cursors.getLong(c, EpisodeColumns.SEASON_ID)
      val collectedAt = Cursors.getLong(c, EpisodeColumns.COLLECTED_AT)

      val collectedShow: CollectedShow?
      var showTraktId = showIdToTraktMap[showId]
      if (showTraktId == null) {
        showTraktId = showHelper.getTraktId(showId)

        showIdToTraktMap[showId] = showTraktId

        collectedShow = CollectedShow(showId)
        showsMap[showTraktId] = collectedShow
      } else {
        collectedShow = showsMap[showTraktId]
      }

      var localCollectedSeason = collectedShow!!.seasons[season]
      if (localCollectedSeason == null) {
        localCollectedSeason = LocalCollectedSeason(seasonId)
        collectedShow.seasons[season] = localCollectedSeason
      }

      val number = Cursors.getInt(c, EpisodeColumns.EPISODE)

      var localCollectedEpisode = localCollectedSeason.episodes[number]
      if (localCollectedEpisode == null) {
        localCollectedEpisode = LocalCollectedEpisode(id, collectedAt)
        localCollectedSeason.episodes[number] = localCollectedEpisode
      }

      episodeIds.add(id)
    }
    c.close()

    val ops = arrayListOf<ContentProviderOperation>()

    for (collectionItem in response) {
      val traktId = collectionItem.show!!.ids.trakt!!

      var collectedShow = showsMap[traktId]

      val showId: Long
      var markedPending = false
      val didShowExist: Boolean
      if (collectedShow == null) {
        val showResult = showHelper.getIdOrCreate(traktId)
        showId = showResult.showId
        didShowExist = !showResult.didCreate
        if (!didShowExist) {
          markedPending = true
        }

        collectedShow = CollectedShow(showId)
        showsMap[traktId] = collectedShow
      } else {
        showId = collectedShow.id
      }

      val lastCollectedMillis = collectionItem.last_collected_at!!.timeInMillis

      ops.add(
        ContentProviderOperation.newUpdate(Shows.withId(collectedShow.id))
          .withValue(ShowColumns.LAST_COLLECTED_AT, lastCollectedMillis)
          .build()
      )

      for (seasonCollectedResponse in collectionItem.seasons!!) {
        var collectedSeason = collectedShow.seasons[seasonCollectedResponse.number]
        if (collectedSeason == null) {
          val seasonResult =
            seasonHelper.getIdOrCreate(collectedShow.id, seasonCollectedResponse.number)
          val seasonId = seasonResult.id

          if (seasonResult.didCreate) {
            if (!markedPending) {
              showHelper.markPending(showId)
              markedPending = true
            }
          }

          collectedSeason = LocalCollectedSeason(seasonId)
          collectedShow.seasons[seasonCollectedResponse.number] = collectedSeason
        }

        for (episode in seasonCollectedResponse.episodes) {
          val collectedAt = episode.collected_at.timeInMillis
          val syncEpisode = collectedSeason.episodes[episode.number]

          if (syncEpisode == null || collectedAt != syncEpisode.collectedAt) {
            val episodeResult =
              episodeHelper.getIdOrCreate(collectedShow.id, collectedSeason.id, episode.number)
            val episodeId = episodeResult.id

            if (episodeResult.didCreate) {
              if (!markedPending) {
                showHelper.markPending(showId)
                markedPending = true
              }
            }

            val builder = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
            val values = ContentValues()
            values.put(EpisodeColumns.IN_COLLECTION, true)
            values.put(EpisodeColumns.COLLECTED_AT, collectedAt)
            builder.withValues(values)
            ops.add(builder.build())
          } else {
            episodeIds.remove(syncEpisode.id)
          }
        }
      }

      apply(ops)
    }

    workManager.enqueueUniqueNow(SyncPendingShowsWorker.TAG, SyncPendingShowsWorker::class.java)

    ops.clear()
    for (episodeId in episodeIds) {
      val builder = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
      val values = ContentValues()
      values.put(EpisodeColumns.IN_COLLECTION, false)
      builder.withValues(values)
      ops.add(builder.build())
    }

    apply(ops)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.EPISODE_COLLECTION, params.userActivityTime)
        .apply()
    }
  }

  private fun apply(ops: ArrayList<ContentProviderOperation>) {
    context.contentResolver.batch(ops)
    ops.clear()
  }

  private class CollectedShow(var id: Long) {
    var seasons = mutableMapOf<Int, LocalCollectedSeason>()
  }

  private class LocalCollectedSeason(var id: Long) {
    var episodes = mutableMapOf<Int, LocalCollectedEpisode>()
  }

  private class LocalCollectedEpisode(var id: Long, var collectedAt: Long)

  data class Params(val userActivityTime: Long = 0L)
}
