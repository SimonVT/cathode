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

package net.simonvt.cathode.ui.shows.watched

import android.content.Context
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.user.SyncWatchedShows
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.ShowWithEpisode
import net.simonvt.cathode.entitymapper.ShowWithEpisodeListMapper
import net.simonvt.cathode.entitymapper.ShowWithEpisodeMapper
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.shows.watched.WatchedShowsFragment.SortBy
import javax.inject.Inject

class WatchedShowsViewModel @Inject constructor(
  context: Context,
  private val syncWatchedShows: SyncWatchedShows
) : RefreshableViewModel() {

  val shows: MappedCursorLiveData<List<ShowWithEpisode>>

  init {
    val sortBy = SortBy.fromValue(
      Settings.get(context).getString(
        Settings.Sort.SHOW_WATCHED,
        SortBy.TITLE.key
      )!!
    )
    shows = MappedCursorLiveData(
      context,
      Shows.SHOWS_WATCHED,
      ShowWithEpisodeMapper.PROJECTION,
      null,
      null,
      sortBy.sortOrder,
      ShowWithEpisodeListMapper()
    )
  }

  fun setSortBy(sortBy: SortBy) {
    shows.setSortOrder(sortBy.sortOrder)
  }

  override suspend fun onRefresh() {
    ActionManager.invokeSync(SyncWatchedShows.key(), syncWatchedShows, Unit)
  }
}
