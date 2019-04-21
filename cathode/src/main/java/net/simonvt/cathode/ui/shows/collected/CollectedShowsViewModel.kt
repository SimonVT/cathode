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

package net.simonvt.cathode.ui.shows.collected

import android.content.Context
import androidx.lifecycle.LiveData
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncShowsCollection
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.entitymapper.ShowWithEpisodeListMapper
import net.simonvt.cathode.entitymapper.ShowWithEpisodeMapper
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.shows.collected.CollectedShowsFragment.SortBy
import javax.inject.Inject

class CollectedShowsViewModel @Inject constructor(
  context: Context,
  private val syncShowsCollection: SyncShowsCollection
) : RefreshableViewModel() {

  private val _shows: MappedCursorLiveData<List<ShowWithEpisode>>
  val shows: LiveData<List<ShowWithEpisode>> get() = _shows

  init {
    val sortBy = SortBy.fromValue(
      Settings.get(context).getString(Settings.Sort.SHOW_WATCHED, SortBy.TITLE.key)!!
    )
    _shows = MappedCursorLiveData(
      context,
      Shows.SHOWS_COLLECTION,
      ShowWithEpisodeMapper.projection,
      null,
      null,
      sortBy.sortOrder,
      ShowWithEpisodeListMapper
    )
  }

  fun setSortBy(sortBy: SortBy) {
    _shows.setSortOrder(sortBy.sortOrder)
  }

  override suspend fun onRefresh() {
    syncShowsCollection.invokeSync(SyncShowsCollection.Params())
  }
}
