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

import net.simonvt.cathode.actions.Action
import net.simonvt.cathode.actions.ActionManager
import javax.inject.Inject

class SyncHiddenItems @Inject constructor(
  private val syncHiddenCalendar: SyncHiddenCalendar,
  private val syncHiddenCollected: SyncHiddenCollected,
  private val syncHiddenRecommendations: SyncHiddenRecommendations,
  private val syncHiddenWatched: SyncHiddenWatched
) : Action<Unit> {

  override suspend fun invoke(params: Unit) {
    val calendarDeferred =
      ActionManager.invokeAsync(SyncHiddenCalendar.key(), syncHiddenCalendar, Unit)
    val collectedDeferred =
      ActionManager.invokeAsync(SyncHiddenCollected.key(), syncHiddenCollected, Unit)
    val recommendationsDeferred =
      ActionManager.invokeAsync(SyncHiddenRecommendations.key(), syncHiddenRecommendations, Unit)
    val watchedDeferred =
      ActionManager.invokeAsync(SyncHiddenWatched.key(), syncHiddenWatched, Unit)

    calendarDeferred.await()
    collectedDeferred.await()
    recommendationsDeferred.await()
    watchedDeferred.await()
  }

  companion object {

    fun key() = "SyncHiddenItems"
  }
}
