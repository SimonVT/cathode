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

package net.simonvt.cathode.ui.show

import android.content.Context
import androidx.lifecycle.LiveData
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.shows.SyncRelatedShows
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Show
import net.simonvt.cathode.entitymapper.ShowListMapper
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.ui.RefreshableViewModel
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter
import javax.inject.Inject

class RelatedShowsViewModel @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val syncRelatedShows: SyncRelatedShows
) : RefreshableViewModel() {

  private var showId = -1L

  lateinit var shows: LiveData<List<Show>>
    private set

  fun setShowId(showId: Long) {
    if (this.showId == -1L) {
      this.showId = showId
      shows = MappedCursorLiveData<List<Show>>(
        context,
        RelatedShows.fromShow(showId),
        ShowDescriptionAdapter.PROJECTION,
        null,
        null,
        null,
        ShowListMapper()
      )
    }
  }

  override suspend fun onRefresh() {
    val traktId = showHelper.getTraktId(showId)
    syncRelatedShows.invokeSync(SyncRelatedShows.Params(traktId))
  }
}
