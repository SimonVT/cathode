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

package net.simonvt.cathode.ui.shows.watchlist

import android.content.Context
import androidx.lifecycle.LiveData
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.user.SyncEpisodeWatchlist
import net.simonvt.cathode.actions.user.SyncShowsWatchlist
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.entitymapper.EpisodeListMapper
import net.simonvt.cathode.entitymapper.EpisodeMapper
import net.simonvt.cathode.entitymapper.ShowListMapper
import net.simonvt.cathode.entitymapper.ShowMapper
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class ShowsWatchlistViewModel @Inject constructor(
  context: Context,
  private val syncShowsWatchlist: SyncShowsWatchlist,
  private val syncEpisodeWatchlist: SyncEpisodeWatchlist
) : RefreshableViewModel() {

  val shows: LiveData<List<Show>>
  val episodes: LiveData<List<Episode>>

  init {
    shows = MappedCursorLiveData(
      context,
      Shows.SHOWS_WATCHLIST,
      ShowMapper.projection,
      null,
      null,
      Shows.DEFAULT_SORT,
      ShowListMapper
    )
    episodes = MappedCursorLiveData(
      context,
      Episodes.EPISODES_IN_WATCHLIST,
      EpisodeMapper.projection,
      null,
      null,
      EpisodeColumns.SHOW_ID + " ASC",
      EpisodeListMapper
    )
  }

  override suspend fun onRefresh() {
    val showsDeferred = syncShowsWatchlist.invokeAsync(SyncShowsWatchlist.Params())
    val episodesDeferred = syncEpisodeWatchlist.invokeAsync(SyncEpisodeWatchlist.Params())

    showsDeferred.await()
    episodesDeferred.await()
  }
}
