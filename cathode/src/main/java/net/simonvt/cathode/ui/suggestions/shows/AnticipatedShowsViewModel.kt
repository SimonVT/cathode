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
import net.simonvt.cathode.actions.shows.SyncAnticipatedShows
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Show
import net.simonvt.cathode.entitymapper.ShowListMapper
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.SuggestionsTimestamps
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter
import net.simonvt.cathode.ui.suggestions.shows.AnticipatedShowsFragment.SortBy
import javax.inject.Inject

class AnticipatedShowsViewModel @Inject constructor(
  private val context: Context,
  private val syncAnticipatedShows: SyncAnticipatedShows
) : RefreshableViewModel() {

  val anticipated: MappedCursorLiveData<List<Show>>

  private var sortBy: SortBy? = null

  init {
    sortBy = SortBy.fromValue(
      Settings.get(context).getString(
        Settings.Sort.SHOW_ANTICIPATED,
        SortBy.ANTICIPATED.key
      )!!
    )
    anticipated = MappedCursorLiveData(
      context,
      Shows.SHOWS_ANTICIPATED,
      ShowDescriptionAdapter.PROJECTION,
      null,
      null,
      sortBy!!.sortOrder,
      ShowListMapper()
    )

    viewModelScope.launch {
      if (System.currentTimeMillis() > SuggestionsTimestamps.get(context).getLong(
          SuggestionsTimestamps.SHOWS_ANTICIPATED,
          0L
        ) + SYNC_INTERNAL
      ) {
        syncAnticipatedShows.invokeAsync(Unit)
      }
    }
  }

  fun setSortBy(sortBy: SortBy) {
    this.sortBy = sortBy
    anticipated.setSortOrder(sortBy.sortOrder)
  }

  override suspend fun onRefresh() {
    syncAnticipatedShows.invokeSync(Unit)
  }

  companion object {

    private const val SYNC_INTERNAL = DateUtils.DAY_IN_MILLIS
  }
}
