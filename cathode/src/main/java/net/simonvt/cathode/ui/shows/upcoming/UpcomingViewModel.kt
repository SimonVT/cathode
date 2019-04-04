/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.shows.upcoming

import android.content.Context
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncWatchedShows
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.ShowWithEpisode
import net.simonvt.cathode.entitymapper.ShowWithEpisodeListMapper
import net.simonvt.cathode.entitymapper.ShowWithEpisodeMapper
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.settings.UpcomingTimePreference
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class UpcomingViewModel @Inject constructor(
  private val context: Context,
  private val upcomingTimePreference: UpcomingTimePreference,
  private val upcomingSortByPreference: UpcomingSortByPreference,
  private val syncWatchedShows: SyncWatchedShows
) : RefreshableViewModel() {

  val shows: MappedCursorLiveData<List<ShowWithEpisode>> = MappedCursorLiveData(
    context,
    Shows.SHOWS_UPCOMING,
    ShowWithEpisodeMapper.PROJECTION,
    null,
    null,
    upcomingSortByPreference.get().sortOrder,
    ShowWithEpisodeListMapper()
  )

  private val upcomingTimeChangeListener =
    UpcomingTimePreference.UpcomingTimeChangeListener { shows.loadData() }

  private val upcomingSortByListener =
    UpcomingSortByPreference.UpcomingSortByListener { sortBy -> shows.setSortOrder(sortBy.sortOrder) }

  init {
    upcomingTimePreference.registerListener(upcomingTimeChangeListener)
    upcomingSortByPreference.registerListener(upcomingSortByListener)
  }

  override fun onCleared() {
    upcomingTimePreference.unregisterListener(upcomingTimeChangeListener)
    upcomingSortByPreference.unregisterListener(upcomingSortByListener)
  }

  override suspend fun onRefresh() {
    syncWatchedShows.invokeSync(SyncWatchedShows.Params())
  }
}
