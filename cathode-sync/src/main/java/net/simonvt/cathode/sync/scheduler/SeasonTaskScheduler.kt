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

import android.content.Context
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import net.simonvt.cathode.api.body.SyncItems
import net.simonvt.cathode.api.util.TimeUtils
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.remote.action.shows.AddSeasonToHistory
import net.simonvt.cathode.remote.action.shows.CollectSeason
import net.simonvt.cathode.remote.action.shows.RemoveSeasonFromHistory
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.work.enqueueNow
import net.simonvt.cathode.work.user.SyncWatchedShowsWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeasonTaskScheduler @Inject
constructor(
  context: Context,
  jobManager: JobManager,
  private val workManager: WorkManager,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper
) : BaseTaskScheduler(context, jobManager) {

  fun addToHistoryNow(seasonId: Long) {
    addToHistory(seasonId, System.currentTimeMillis())
  }

  fun addToHistoryOnRelease(seasonId: Long) {
    addToHistory(seasonId, SyncItems.TIME_RELEASED)
  }

  fun addToHistory(seasonId: Long, watchedAt: Long) {
    val isoWhen = TimeUtils.getIsoTime(watchedAt)
    addToHistory(seasonId, isoWhen)
  }

  fun addToHistory(
    seasonId: Long,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int
  ) {
    addToHistory(seasonId, TimeUtils.getMillis(year, month, day, hour, minute))
  }

  fun addToHistory(seasonId: Long, watchedAt: String) {
    scope.launch {
      var watched = SeasonDatabaseHelper.WATCHED_RELEASE
      if (SyncItems.TIME_RELEASED != watchedAt) {
        watched = TimeUtils.getMillis(watchedAt)
      }

      seasonHelper.addToHistory(seasonId, watched)

      if (TraktLinkSettings.isLinked(context)) {
        val showId = seasonHelper.getShowId(seasonId)
        val traktId = showHelper.getTraktId(showId)
        val seasonNumber = seasonHelper.getNumber(seasonId)
        queue(AddSeasonToHistory(traktId, seasonNumber, watchedAt))
        // No documentation on how exactly the trakt endpoint is implemented, so sync after.
        workManager.enqueueNow(SyncWatchedShowsWorker::class.java)
      }
    }
  }

  fun removeFromHistory(seasonId: Long) {
    scope.launch {
      seasonHelper.removeFromHistory(seasonId)

      if (TraktLinkSettings.isLinked(context)) {
        val showId = seasonHelper.getShowId(seasonId)
        val traktId = showHelper.getTraktId(showId)
        val seasonNumber = seasonHelper.getNumber(seasonId)
        queue(RemoveSeasonFromHistory(traktId, seasonNumber))
      }
    }
  }

  fun setInCollection(seasonId: Long, inCollection: Boolean) {
    scope.launch {
      var collectedAt: String? = null
      var collectedAtMillis = 0L
      if (inCollection) {
        collectedAt = TimeUtils.getIsoTime()
        collectedAtMillis = TimeUtils.getMillis(collectedAt)
      }

      seasonHelper.setIsInCollection(seasonId, inCollection, collectedAtMillis)

      if (TraktLinkSettings.isLinked(context)) {
        val showId = seasonHelper.getShowId(seasonId)
        val traktId = showHelper.getTraktId(showId)
        val seasonNumber = seasonHelper.getNumber(seasonId)
        queue(CollectSeason(traktId, seasonNumber, inCollection, collectedAt))
      }
    }
  }
}
