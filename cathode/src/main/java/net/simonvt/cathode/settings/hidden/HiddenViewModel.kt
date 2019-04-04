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

package net.simonvt.cathode.settings.hidden

import android.content.Context
import androidx.lifecycle.LiveData
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncHiddenItems
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Movie
import net.simonvt.cathode.common.entity.Show
import net.simonvt.cathode.entitymapper.MovieListMapper
import net.simonvt.cathode.entitymapper.ShowListMapper
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class HiddenViewModel @Inject constructor(
  context: Context,
  private val syncHiddenItems: SyncHiddenItems
) : RefreshableViewModel() {

  val showsCalendar: LiveData<List<Show>>
  val showsWatched: LiveData<List<Show>>
  val showsCollected: LiveData<List<Show>>
  val moviesCalendar: LiveData<List<Movie>>

  init {

    showsCalendar = MappedCursorLiveData(
      context,
      Shows.SHOWS,
      HiddenItemsAdapter.PROJECTION_SHOW,
      ShowColumns.HIDDEN_CALENDAR + "=1",
      null,
      Shows.SORT_TITLE,
      ShowListMapper()
    )
    showsWatched = MappedCursorLiveData(
      context,
      Shows.SHOWS,
      HiddenItemsAdapter.PROJECTION_SHOW,
      ShowColumns.HIDDEN_WATCHED + "=1",
      null,
      Shows.SORT_TITLE,
      ShowListMapper()
    )
    showsCollected = MappedCursorLiveData(
      context,
      Shows.SHOWS,
      HiddenItemsAdapter.PROJECTION_SHOW,
      ShowColumns.HIDDEN_COLLECTED + "=1",
      null,
      Shows.SORT_TITLE,
      ShowListMapper()
    )
    moviesCalendar = MappedCursorLiveData(
      context,
      Movies.MOVIES,
      HiddenItemsAdapter.PROJECTION_MOVIES,
      MovieColumns.HIDDEN_CALENDAR + "=1",
      null,
      Movies.SORT_TITLE,
      MovieListMapper()
    )
  }

  override suspend fun onRefresh() {
    syncHiddenItems.invokeSync(SyncHiddenItems.Params())
  }
}
