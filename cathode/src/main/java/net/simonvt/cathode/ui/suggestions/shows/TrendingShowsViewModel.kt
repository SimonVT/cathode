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

package net.simonvt.cathode.ui.suggestions.shows

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.shows.SyncTrendingShows
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.entitymapper.ShowListMapper
import net.simonvt.cathode.entitymapper.ShowMapper
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.SuggestionsTimestamps
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.suggestions.shows.TrendingShowsFragment.SortBy
import javax.inject.Inject

class TrendingShowsViewModel @Inject constructor(
  private val context: Context,
  private val syncTrendingShows: SyncTrendingShows
) : RefreshableViewModel() {

  val trending: MappedCursorLiveData<List<Show>>

  private var sortBy: SortBy

  init {
    sortBy = SortBy.fromValue(
      Settings.get(context).getString(
        Settings.Sort.SHOW_TRENDING,
        SortBy.VIEWERS.key
      )!!
    )
    trending = MappedCursorLiveData(
      context,
      Shows.SHOWS_TRENDING,
      ShowMapper.projection,
      null,
      null,
      sortBy.sortOrder,
      ShowListMapper
    )

    viewModelScope.launch {
      if (System.currentTimeMillis() > SuggestionsTimestamps.get(context).getLong(
          SuggestionsTimestamps.SHOWS_TRENDING,
          0L
        ) + SYNC_INTERNAL
      ) {
        syncTrendingShows.invokeAsync(Unit)
      }
    }
  }

  fun setSortBy(sortBy: SortBy) {
    this.sortBy = sortBy
    trending.setSortOrder(sortBy.sortOrder)
  }

  override suspend fun onRefresh() {
    syncTrendingShows.invokeSync(Unit)
  }

  companion object {

    const val SYNC_INTERNAL = 6 * DateUtils.HOUR_IN_MILLIS
  }
}
