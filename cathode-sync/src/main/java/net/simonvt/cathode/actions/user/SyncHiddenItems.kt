/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import net.simonvt.cathode.actions.Action
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.user.SyncHiddenItems.Params
import net.simonvt.cathode.settings.TraktTimestamps
import javax.inject.Inject

class SyncHiddenItems @Inject constructor(
  private val context: Context,
  private val syncHiddenCalendar: SyncHiddenCalendar,
  private val syncHiddenCollected: SyncHiddenCollected,
  private val syncHiddenRecommendations: SyncHiddenRecommendations,
  private val syncHiddenWatched: SyncHiddenWatched
) : Action<Params> {

  override fun key(params: Params): String = "SyncHiddenItems"

  override suspend fun invoke(params: Params) {
    val calendarDeferred = syncHiddenCalendar.invokeAsync(Unit)
    val collectedDeferred = syncHiddenCollected.invokeAsync(Unit)
    val recommendationsDeferred = syncHiddenRecommendations.invokeAsync(Unit)
    val watchedDeferred = syncHiddenWatched.invokeAsync(Unit)

    calendarDeferred.await()
    collectedDeferred.await()
    recommendationsDeferred.await()
    watchedDeferred.await()

    if (params.showLastHidden > 0L || params.movieLastHidden > 0L) {
      TraktTimestamps.getSettings(context)
        .edit()
        .putLong(TraktTimestamps.SHOW_HIDE, params.showLastHidden)
        .putLong(TraktTimestamps.MOVIE_HIDE, params.movieLastHidden)
        .apply()
    }
  }

  data class Params(val showLastHidden: Long = 0L, val movieLastHidden: Long = 0L)
}
