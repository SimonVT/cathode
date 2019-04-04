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
import net.simonvt.cathode.actions.user.SyncWatchedShows.Params
import net.simonvt.cathode.api.entity.WatchedItem
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
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class SyncWatchedShows @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val syncService: SyncService,
  private val workManager: WorkManager
) : CallAction<Params, List<WatchedItem>>() {

  override fun key(params: Params): String = "SyncWatchedShows"

  override fun getCall(params: Params): Call<List<WatchedItem>> = syncService.getWatchedShows()

  override suspend fun handleResponse(params: Params, response: List<WatchedItem>) {
    val c = context.contentResolver.query(
      Episodes.EPISODES,
      arrayOf(
        EpisodeColumns.ID,
        EpisodeColumns.SHOW_ID,
        EpisodeColumns.SEASON,
        EpisodeColumns.SEASON_ID,
        EpisodeColumns.EPISODE,
        EpisodeColumns.LAST_WATCHED_AT
      ),
      EpisodeColumns.WATCHED
    )

    val showsMap = mutableMapOf<Long, WatchedShow>()
    val showIdToTraktMap = mutableMapOf<Long, Long>()
    val episodeIds = mutableListOf<Long>()

    while (c.moveToNext()) {
      val id = Cursors.getLong(c, EpisodeColumns.ID)
      val showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID)
      val season = Cursors.getInt(c, EpisodeColumns.SEASON)
      val seasonId = Cursors.getLong(c, EpisodeColumns.SEASON_ID)
      val lastWatchedAt = Cursors.getLong(c, EpisodeColumns.LAST_WATCHED_AT)

      val watchedShow: WatchedShow?
      var showTraktId = showIdToTraktMap[showId]
      if (showTraktId == null) {
        showTraktId = showHelper.getTraktId(showId)

        showIdToTraktMap[showId] = showTraktId

        watchedShow = WatchedShow(showId)
        showsMap[showTraktId] = watchedShow
      } else {
        watchedShow = showsMap[showTraktId]
      }

      var syncSeason = watchedShow!!.seasons[season]
      if (syncSeason == null) {
        syncSeason = LocalWatchedSeason(seasonId)
        watchedShow.seasons[season] = syncSeason
      }

      val number = Cursors.getInt(c, EpisodeColumns.EPISODE)

      var syncEpisode = syncSeason.episodes[number]
      if (syncEpisode == null) {
        syncEpisode = LocalWatchedEpisode(id, lastWatchedAt)
        syncSeason.episodes[number] = syncEpisode
      }

      episodeIds.add(id)
    }
    c.close()

    val ops = arrayListOf<ContentProviderOperation>()

    Timber.d("Processing items")
    for (watchedItem in response) {
      val traktId = watchedItem.show!!.ids.trakt!!
      Timber.d("Processing: %d", traktId)

      var watchedShow = showsMap[traktId]

      val showId: Long
      var markedPending = false
      val didShowExist: Boolean
      if (watchedShow == null) {
        val showResult = showHelper.getIdOrCreate(traktId)
        showId = showResult.showId
        didShowExist = !showResult.didCreate
        if (!didShowExist) {
          markedPending = true
        }

        watchedShow = WatchedShow(showId)
        showsMap[traktId] = watchedShow
      } else {
        showId = watchedShow.id
      }

      val lastWatchedMillis = watchedItem.last_watched_at.timeInMillis

      ops.add(
        ContentProviderOperation.newUpdate(Shows.withId(watchedShow.id))
          .withValue(ShowColumns.LAST_WATCHED_AT, lastWatchedMillis)
          .build()
      )

      for ((seasonNumber, episodes) in watchedItem.seasons!!) {
        var localWatchedSeason = watchedShow.seasons[seasonNumber]
        if (localWatchedSeason == null) {
          val seasonResult = seasonHelper.getIdOrCreate(watchedShow.id, seasonNumber)
          val seasonId = seasonResult.id
          if (seasonResult.didCreate) {
            if (!markedPending) {
              showHelper.markPending(showId)
              markedPending = true
            }
          }
          localWatchedSeason = LocalWatchedSeason(seasonId)
          watchedShow.seasons[seasonNumber] = localWatchedSeason
        }

        for (watchedEpisode in episodes) {
          val lastWatchedAt = watchedEpisode.last_watched_at.timeInMillis
          val syncEpisode = localWatchedSeason.episodes[watchedEpisode.number]

          if (syncEpisode == null) {
            val episodeResult =
              episodeHelper.getIdOrCreate(
                watchedShow.id,
                localWatchedSeason.id,
                watchedEpisode.number
              )
            val episodeId = episodeResult.id
            if (episodeResult.didCreate) {
              if (!markedPending) {
                showHelper.markPending(showId)
                markedPending = true
              }
            }

            val builder = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
            val values = ContentValues()
            values.put(EpisodeColumns.WATCHED, true)
            values.put(EpisodeColumns.LAST_WATCHED_AT, lastWatchedAt)
            builder.withValues(values)
            ops.add(builder.build())
          } else {
            episodeIds.remove(syncEpisode.id)

            if (lastWatchedAt != syncEpisode.lastWatched) {
              val builder = ContentProviderOperation.newUpdate(Episodes.withId(syncEpisode.id))
              val values = ContentValues()
              values.put(EpisodeColumns.LAST_WATCHED_AT, lastWatchedAt)
              builder.withValues(values)
              ops.add(builder.build())
            }
          }
        }
      }

      if (markedPending) {
        workManager.enqueueUniqueNow(SyncPendingShowsWorker.TAG, SyncPendingShowsWorker::class.java)
      }

      apply(ops)
    }

    for (episodeId in episodeIds) {
      val builder = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
      val values = ContentValues()
      values.put(EpisodeColumns.WATCHED, false)
      builder.withValues(values)
      ops.add(builder.build())
    }

    apply(ops)

    if (params.userActivityTime > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.EPISODE_WATCHED, params.userActivityTime)
        .apply()
    }
  }

  private fun apply(ops: ArrayList<ContentProviderOperation>): Boolean {
    context.contentResolver.batch(ops)
    ops.clear()
    return true
  }

  private class WatchedShow(var id: Long) {
    var seasons = mutableMapOf<Int, LocalWatchedSeason>()
  }

  private class LocalWatchedSeason(var id: Long) {
    var episodes = mutableMapOf<Int, LocalWatchedEpisode>()
  }

  private class LocalWatchedEpisode(var id: Long, var lastWatched: Long)

  data class Params(val userActivityTime: Long = 0L)
}
