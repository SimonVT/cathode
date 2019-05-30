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
package net.simonvt.cathode.sync.scheduler

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.text.format.DateUtils
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import net.simonvt.cathode.api.body.SyncItems
import net.simonvt.cathode.api.service.CheckinService
import net.simonvt.cathode.api.util.TimeUtils
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.event.ErrorEvent
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.remote.action.RemoveHistoryItem
import net.simonvt.cathode.remote.action.shows.AddEpisodeToHistory
import net.simonvt.cathode.remote.action.shows.CollectEpisode
import net.simonvt.cathode.remote.action.shows.RateEpisode
import net.simonvt.cathode.remote.action.shows.RemoveEpisodeFromHistory
import net.simonvt.cathode.remote.action.shows.WatchlistEpisode
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.R
import net.simonvt.cathode.sync.trakt.CheckIn
import net.simonvt.cathode.work.enqueueNow
import net.simonvt.cathode.work.user.SyncWatchingWorker
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeTaskScheduler @Inject constructor(
  context: Context,
  jobManager: JobManager,
  private val workManager: WorkManager,
  private val showHelper: ShowDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val checkinService: CheckinService,
  private val checkIn: CheckIn
) : BaseTaskScheduler(context, jobManager) {

  fun addToHistoryNow(episodeId: Long) {
    addToHistory(episodeId, System.currentTimeMillis())
  }

  private fun getOlderEpisodes(episodeId: Long): Cursor {
    val showId = episodeHelper.getShowId(episodeId)
    val season = episodeHelper.getSeason(episodeId)
    val episode = episodeHelper.getNumber(episodeId)

    return context.contentResolver.query(
      Episodes.EPISODES,
      arrayOf(EpisodeColumns.ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE),
      EpisodeColumns.SHOW_ID + "=" + showId + " AND " +
          EpisodeColumns.WATCHED + "=0 AND " +
          EpisodeColumns.SEASON + ">0 AND ((" +
          EpisodeColumns.SEASON + "=" + season + " AND " +
          EpisodeColumns.EPISODE + "<" + episode + ") OR " +
          EpisodeColumns.SEASON + "<" + season + ")"
    )
  }

  fun addOlderToHistoryNow(episodeId: Long) {
    scope.launch {
      val episodes = getOlderEpisodes(episodeId)
      while (episodes.moveToNext()) {
        val id = episodes.getLong(EpisodeColumns.ID)
        addToHistoryNow(id)
      }
      episodes.close()
    }
  }

  fun addToHistoryOnRelease(episodeId: Long) {
    addToHistory(episodeId, SyncItems.TIME_RELEASED)
  }

  fun addOlderToHistoryOnRelease(episodeId: Long) {
    scope.launch {
      val episodes = getOlderEpisodes(episodeId)
      while (episodes.moveToNext()) {
        val id = episodes.getLong(EpisodeColumns.ID)
        addToHistory(id, SyncItems.TIME_RELEASED)
      }
      episodes.close()
    }
  }

  fun addOlderToHistory(
    episodeId: Long,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int
  ) {
    scope.launch {
      val episodes = getOlderEpisodes(episodeId)
      while (episodes.moveToNext()) {
        val id = episodes.getLong(EpisodeColumns.ID)
        addToHistory(id, TimeUtils.getMillis(year, month, day, hour, minute))
      }
      episodes.close()
    }
  }

  fun addToHistory(episodeId: Long, watchedAt: Long) {
    val isoWhen = TimeUtils.getIsoTime(watchedAt)
    addToHistory(episodeId, isoWhen)
  }

  fun addToHistory(episodeId: Long, year: Int, month: Int, day: Int, hour: Int, minute: Int) {
    addToHistory(episodeId, TimeUtils.getMillis(year, month, day, hour, minute))
  }

  fun addToHistory(episodeId: Long, watchedAt: String) {
    scope.launch {
      val c = context.contentResolver.query(
        Episodes.withId(episodeId),
        arrayOf(EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE)
      )
      c.moveToFirst()
      val showId = c.getLong(EpisodeColumns.SHOW_ID)
      val traktId = showHelper.getTraktId(showId)
      val season = c.getInt(EpisodeColumns.SEASON)
      val number = c.getInt(EpisodeColumns.EPISODE)
      c.close()

      if (SyncItems.TIME_RELEASED == watchedAt) {
        episodeHelper.addToHistory(episodeId, EpisodeDatabaseHelper.WATCHED_RELEASE)
      } else {
        episodeHelper.addToHistory(episodeId, TimeUtils.getMillis(watchedAt))
      }

      if (TraktLinkSettings.isLinked(context)) {
        queue(AddEpisodeToHistory(traktId, season, number, watchedAt))
      }
    }
  }

  fun removeFromHistory(episodeId: Long) {
    scope.launch {
      episodeHelper.removeFromHistory(episodeId)

      if (TraktLinkSettings.isLinked(context)) {
        val c = context.contentResolver.query(
          Episodes.withId(episodeId),
          arrayOf(EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE)
        )
        c.moveToFirst()
        val showId = c.getLong(EpisodeColumns.SHOW_ID)
        val traktId = showHelper.getTraktId(showId)
        val season = c.getInt(EpisodeColumns.SEASON)
        val number = c.getInt(EpisodeColumns.EPISODE)
        c.close()

        queue(RemoveEpisodeFromHistory(traktId, season, number))
      }
    }
  }

  fun removeHistoryItem(episodeId: Long, historyId: Long, lastItem: Boolean) {
    scope.launch {
      if (lastItem) {
        episodeHelper.removeFromHistory(episodeId)
      }

      if (TraktLinkSettings.isLinked(context)) {
        queue(RemoveHistoryItem(historyId))
      }
    }
  }

  fun checkin(
    episodeId: Long,
    message: String?,
    facebook: Boolean,
    twitter: Boolean,
    tumblr: Boolean
  ) {
    scope.launch {
      val watching = context.contentResolver.query(
        Episodes.EPISODE_WATCHING,
        arrayOf(EpisodeColumns.ID, EpisodeColumns.EXPIRES_AT),
        null,
        null,
        null
      )

      val currentTime = System.currentTimeMillis()
      var expires: Long = 0
      if (watching!!.moveToFirst()) {
        expires = watching.getLong(EpisodeColumns.EXPIRES_AT)
      }

      val episode = context.contentResolver.query(
        Episodes.withId(episodeId),
        arrayOf(
          EpisodeColumns.SHOW_ID,
          EpisodeColumns.TITLE,
          EpisodeColumns.SEASON,
          EpisodeColumns.EPISODE,
          EpisodeColumns.WATCHED
        )
      )
      episode.moveToFirst()
      val showId = episode.getLong(EpisodeColumns.SHOW_ID)
      val season = episode.getInt(EpisodeColumns.SEASON)
      val number = episode.getInt(EpisodeColumns.EPISODE)
      val watched = episode.getBoolean(EpisodeColumns.WATCHED)
      val title = DataHelper.getEpisodeTitle(context, episode, season, number, watched)
      episode.close()

      val show = context.contentResolver.query(
        Shows.withId(showId),
        arrayOf(ShowColumns.RUNTIME),
        null,
        null,
        null
      )
      show!!.moveToFirst()
      val runtime = show.getInt(ShowColumns.RUNTIME)
      val watchSlop = (runtime.toFloat() * DateUtils.MINUTE_IN_MILLIS.toFloat() * 0.8f).toLong()
      show.close()

      if (watching.count == 0 || expires - watchSlop < currentTime && expires > 0) {
        if (checkIn.episode(episodeId, message, facebook, twitter, tumblr)) {
          episodeHelper.checkIn(episodeId)
        }
      } else {
        ErrorEvent.post(context.getString(R.string.checkin_error_watching, title))
      }

      watching.close()

      workManager.enqueueNow(SyncWatchingWorker::class.java)
    }
  }

  fun cancelCheckin() {
    scope.launch {
      var episode: Cursor? = null
      try {
        episode = context.contentResolver.query(
          Episodes.EPISODE_WATCHING,
          arrayOf(EpisodeColumns.ID, EpisodeColumns.STARTED_AT, EpisodeColumns.EXPIRES_AT)
        )

        if (episode.moveToFirst()) {
          val id = episode.getLong(EpisodeColumns.ID)
          val startedAt = episode.getLong(EpisodeColumns.STARTED_AT)
          val expiresAt = episode.getLong(EpisodeColumns.EXPIRES_AT)

          val values = ContentValues()
          values.put(EpisodeColumns.CHECKED_IN, false)
          context.contentResolver.update(Episodes.EPISODE_WATCHING, values, null, null)

          try {
            val call = checkinService.deleteCheckin()
            val response = call.execute()
            if (response.isSuccessful) {
              return@launch
            }
          } catch (e: IOException) {
            Timber.d(e)
          }

          ErrorEvent.post(context.getString(R.string.checkin_cancel_error))

          values.clear()
          values.put(EpisodeColumns.CHECKED_IN, true)
          values.put(EpisodeColumns.STARTED_AT, startedAt)
          values.put(EpisodeColumns.EXPIRES_AT, expiresAt)
          context.contentResolver.update(Episodes.withId(id), values, null, null)

          workManager.enqueueNow(SyncWatchingWorker::class.java)
        }
      } finally {
        episode?.close()
      }
    }
  }

  /**
   * Add episodes to user library collection.
   *
   * @param episodeId The database id of the episode.
   */
  fun setIsInCollection(episodeId: Long, inCollection: Boolean) {
    scope.launch {
      var collectedAt: String? = null
      var collectedAtMillis = 0L
      if (inCollection) {
        collectedAt = TimeUtils.getIsoTime()
        collectedAtMillis = TimeUtils.getMillis(collectedAt)
      }

      episodeHelper.setInCollection(episodeId, inCollection, collectedAtMillis)

      if (TraktLinkSettings.isLinked(context)) {
        val c = context.contentResolver.query(
          Episodes.withId(episodeId),
          arrayOf(EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE)
        )
        c.moveToFirst()
        val showId = c.getLong(EpisodeColumns.SHOW_ID)
        val traktId = showHelper.getTraktId(showId)
        val season = c.getInt(EpisodeColumns.SEASON)
        val number = c.getInt(EpisodeColumns.EPISODE)
        c.close()

        queue(CollectEpisode(traktId, season, number, inCollection, collectedAt))
      }
    }
  }

  fun setIsInWatchlist(episodeId: Long, inWatchlist: Boolean) {
    scope.launch {
      var listedAt: String? = null
      var listeddAtMillis = 0L
      if (inWatchlist) {
        listedAt = TimeUtils.getIsoTime()
        listeddAtMillis = TimeUtils.getMillis(listedAt)
      }

      episodeHelper.setIsInWatchlist(episodeId, inWatchlist, listeddAtMillis)

      if (TraktLinkSettings.isLinked(context)) {
        val c = context.contentResolver.query(
          Episodes.withId(episodeId),
          arrayOf(EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE)
        )
        c.moveToFirst()
        val showId = c.getLong(EpisodeColumns.SHOW_ID)
        val traktId = showHelper.getTraktId(showId)
        val season = c.getInt(EpisodeColumns.SEASON)
        val number = c.getInt(EpisodeColumns.EPISODE)
        c.close()

        queue(WatchlistEpisode(traktId, season, number, inWatchlist, listedAt))
      }
    }
  }

  /**
   * Rate an episode on trakt. Depending on the user settings, this will also send out social
   * updates to facebook,
   * twitter, and tumblr.
   *
   * @param episodeId The database id of the episode.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  fun rate(episodeId: Long, rating: Int) {
    scope.launch {
      val ratedAt = TimeUtils.getIsoTime()
      val ratedAtMillis = TimeUtils.getMillis(ratedAt)

      val showId = episodeHelper.getShowId(episodeId)
      val showTraktId = showHelper.getTraktId(showId)
      val c = context.contentResolver.query(
        Episodes.withId(episodeId),
        arrayOf(EpisodeColumns.EPISODE, EpisodeColumns.SEASON)
      )

      if (c.moveToFirst()) {
        val episode = c.getInt(EpisodeColumns.EPISODE)
        val season = c.getInt(EpisodeColumns.SEASON)

        val values = ContentValues()
        values.put(EpisodeColumns.USER_RATING, rating)
        values.put(EpisodeColumns.RATED_AT, ratedAtMillis)
        context.contentResolver.update(Episodes.withId(episodeId), values, null, null)

        if (TraktLinkSettings.isLinked(context)) {
          queue(RateEpisode(showTraktId, season, episode, rating, ratedAt))
        }
      }
      c.close()
    }
  }
}
