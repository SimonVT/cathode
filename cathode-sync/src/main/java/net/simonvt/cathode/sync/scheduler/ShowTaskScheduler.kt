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
package net.simonvt.cathode.sync.scheduler

import android.content.ContentValues
import android.content.Context
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import net.simonvt.cathode.api.body.SyncItems
import net.simonvt.cathode.api.util.TimeUtils
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.remote.action.shows.AddShowToHistory
import net.simonvt.cathode.remote.action.shows.CalendarHideShow
import net.simonvt.cathode.remote.action.shows.CollectedHideShow
import net.simonvt.cathode.remote.action.shows.DismissShowRecommendation
import net.simonvt.cathode.remote.action.shows.RateShow
import net.simonvt.cathode.remote.action.shows.WatchedHideShow
import net.simonvt.cathode.remote.action.shows.WatchlistShow
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.work.enqueueNow
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import net.simonvt.cathode.work.user.SyncWatchedShowsWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowTaskScheduler @Inject constructor(
  context: Context,
  jobManager: JobManager,
  private val workManager: WorkManager,
  private val episodeScheduler: EpisodeTaskScheduler,
  private val showHelper: ShowDatabaseHelper
) : BaseTaskScheduler(context, jobManager) {

  fun collectedNext(showId: Long) {
    scope.launch {
      val c = context.contentResolver.query(
        Episodes.fromShow(showId),
        arrayOf(EpisodeColumns.ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE),
        "inCollection=0 AND season<>0",
        null,
        EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1"
      )!!

      if (c.moveToNext()) {
        val episodeId = c.getLong(EpisodeColumns.ID)
        episodeScheduler.setIsInCollection(episodeId, true)
      }

      c.close()
    }
  }

  fun addToHistoryNow(showId: Long) {
    addToHistory(showId, System.currentTimeMillis())
  }

  fun addToHistoryOnRelease(showId: Long) {
    addToHistory(showId, SyncItems.TIME_RELEASED)
  }

  fun addToHistory(showId: Long, watchedAt: Long) {
    val isoWhen = TimeUtils.getIsoTime(watchedAt)
    addToHistory(showId, isoWhen)
  }

  fun addToHistory(
    showId: Long,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int
  ) {
    addToHistory(showId, TimeUtils.getMillis(year, month, day, hour, minute))
  }

  fun addToHistory(showId: Long, watchedAt: String) {
    scope.launch {
      if (SyncItems.TIME_RELEASED == watchedAt) {
        showHelper.addToHistory(showId, ShowDatabaseHelper.WATCHED_RELEASE)
      } else {
        showHelper.addToHistory(showId, TimeUtils.getMillis(watchedAt))
      }

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = showHelper.getTraktId(showId)
        queue(AddShowToHistory(traktId, watchedAt))
        // No documentation on how exactly the trakt endpoint is implemented, so sync after.
        workManager.enqueueNow(SyncWatchedShowsWorker::class.java)
      }
    }
  }

  fun setIsInWatchlist(showId: Long, inWatchlist: Boolean) {
    scope.launch {
      val c = context.contentResolver.query(
        Shows.withId(showId),
        arrayOf(ShowColumns.TRAKT_ID, ShowColumns.EPISODE_COUNT)
      )

      if (c.moveToFirst()) {
        var listedAt: String? = null
        var listedAtMillis = 0L
        if (inWatchlist) {
          listedAt = TimeUtils.getIsoTime()
          listedAtMillis = TimeUtils.getMillis(listedAt)
        }

        val traktId = c.getLong(ShowColumns.TRAKT_ID)
        showHelper.setIsInWatchlist(showId, inWatchlist, listedAtMillis)

        val episodeCount = c.getInt(ShowColumns.EPISODE_COUNT)
        if (episodeCount == 0) {
          workManager.enqueueUniqueNow(
            SyncPendingShowsWorker.TAG,
            SyncPendingShowsWorker::class.java
          )
        }

        if (TraktLinkSettings.isLinked(context)) {
          queue(WatchlistShow(traktId, inWatchlist, listedAt))
        }
      }

      c.close()
    }
  }

  fun dismissRecommendation(showId: Long) {
    scope.launch {
      val traktId = showHelper.getTraktId(showId)

      val values = ContentValues()
      values.put(ShowColumns.RECOMMENDATION_INDEX, -1)
      context.contentResolver.update(Shows.withId(showId), values, null, null)

      queue(DismissShowRecommendation(traktId))
    }
  }

  /**
   * Rate a show on trakt. Depending on the user settings, this will also send out social updates
   * to facebook,
   * twitter, and tumblr.
   *
   * @param showId The database id of the show.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  fun rate(showId: Long, rating: Int) {
    scope.launch {
      val ratedAt = TimeUtils.getIsoTime()
      val ratedAtMillis = TimeUtils.getMillis(ratedAt)

      val values = ContentValues()
      values.put(ShowColumns.USER_RATING, rating)
      values.put(ShowColumns.RATED_AT, ratedAtMillis)
      context.contentResolver.update(Shows.withId(showId), values, null, null)

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = showHelper.getTraktId(showId)
        queue(RateShow(traktId, rating, ratedAt))
      }
    }
  }

  fun hideFromCalendar(showId: Long, hidden: Boolean) {
    scope.launch {
      val values = ContentValues()
      values.put(ShowColumns.HIDDEN_CALENDAR, hidden)
      context.contentResolver.update(Shows.withId(showId), values, null, null)

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = showHelper.getTraktId(showId)
        queue(CalendarHideShow(traktId, hidden))
      }
    }
  }

  fun hideFromWatched(showId: Long, hidden: Boolean) {
    scope.launch {
      val values = ContentValues()
      values.put(ShowColumns.HIDDEN_WATCHED, hidden)
      context.contentResolver.update(Shows.withId(showId), values, null, null)

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = showHelper.getTraktId(showId)
        queue(WatchedHideShow(traktId, hidden))
      }
    }
  }

  fun hideFromCollected(showId: Long, hidden: Boolean) {
    scope.launch {
      val values = ContentValues()
      values.put(ShowColumns.HIDDEN_COLLECTED, hidden)
      context.contentResolver.update(Shows.withId(showId), values, null, null)

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = showHelper.getTraktId(showId)
        queue(CollectedHideShow(traktId, hidden))
      }
    }
  }
}
