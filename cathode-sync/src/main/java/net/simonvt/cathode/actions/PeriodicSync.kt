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
package net.simonvt.cathode.actions

import android.content.Context
import android.text.format.DateUtils
import androidx.work.WorkManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.movies.MarkSyncUserMovies
import net.simonvt.cathode.actions.movies.SyncPendingMovies
import net.simonvt.cathode.actions.shows.MarkSyncUserShows
import net.simonvt.cathode.actions.shows.SyncPendingShows
import net.simonvt.cathode.actions.tmdb.SyncConfiguration
import net.simonvt.cathode.actions.user.SyncUserActivity
import net.simonvt.cathode.actions.user.SyncUserProfile
import net.simonvt.cathode.actions.user.SyncUserSettings
import net.simonvt.cathode.actions.user.SyncWatching
import net.simonvt.cathode.settings.Timestamps
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.movies.SyncUpdatedMoviesWorker
import net.simonvt.cathode.work.shows.SyncUpdatedShowsWorker
import javax.inject.Inject

class PeriodicSync @Inject constructor(
  private val context: Context,
  private val workManager: WorkManager,
  private val syncUserSettings: SyncUserSettings,
  private val syncUserProfile: SyncUserProfile,
  private val syncConfiguration: SyncConfiguration,
  private val syncUserActivity: SyncUserActivity,
  private val syncWatching: SyncWatching,
  private val markSyncUserShows: MarkSyncUserShows,
  private val markSyncUserMovies: MarkSyncUserMovies,
  private val syncPendingShows: SyncPendingShows,
  private val syncPendingMovies: SyncPendingMovies
) : Action<Unit> {

  override fun key(params: Unit): String = "PeriodicSync"

  override suspend fun invoke(params: Unit) = coroutineScope {
    val actions = mutableListOf<Deferred<*>>()

    val currentTime = System.currentTimeMillis()
    val lastConfigSync = Timestamps.get(context).getLong(Timestamps.LAST_CONFIG_SYNC, 0L)
    val lastShowSync = Timestamps.get(context).getLong(Timestamps.SHOWS_LAST_UPDATED, 0L)
    val lastMovieSync = Timestamps.get(context).getLong(Timestamps.MOVIES_LAST_UPDATED, 0L)

    if (lastConfigSync + DateUtils.DAY_IN_MILLIS < currentTime) {
      actions += async {
        val configuration = syncConfiguration.invokeAsync(Unit)

        if (TraktLinkSettings.isLinked(context)) {
          val userSettings = syncUserSettings.invokeAsync(Unit)
          val userProfile = syncUserProfile.invokeAsync(Unit)

          userSettings.await()
          userProfile.await()
        }

        configuration.await()
        Timestamps.get(context).edit().putLong(Timestamps.LAST_CONFIG_SYNC, currentTime).apply()
      }
    }
    if (lastShowSync == 0L) {
      Timestamps.get(context)
        .edit()
        .putLong(Timestamps.SHOWS_LAST_UPDATED, currentTime)
        .apply()
    } else if (lastShowSync + UPDATE_SYNC_DELAY < currentTime) {
      workManager.enqueueUniqueNow(SyncUpdatedShowsWorker.TAG, SyncUpdatedShowsWorker::class.java)
    }
    if (lastMovieSync == 0L) {
      Timestamps.get(context)
        .edit()
        .putLong(Timestamps.MOVIES_LAST_UPDATED, currentTime)
        .apply()
    } else if (lastMovieSync + UPDATE_SYNC_DELAY < currentTime) {
      workManager.enqueueUniqueNow(SyncUpdatedMoviesWorker.TAG, SyncUpdatedMoviesWorker::class.java)
    }

    actions += markSyncUserShows.invokeAsync(Unit)
    actions += markSyncUserMovies.invokeAsync(Unit)

    if (TraktLinkSettings.isLinked(context)) {
      actions += syncUserActivity.invokeAsync(Unit)
      actions += syncWatching.invokeAsync(Unit)
    }

    actions += syncPendingShows.invokeAsync(Unit)
    actions += syncPendingMovies.invokeAsync(Unit)

    actions.forEach { it.await() }
  }

  companion object {
    private const val UPDATE_SYNC_DELAY = 7 * DateUtils.DAY_IN_MILLIS
  }
}
