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
import net.simonvt.cathode.actions.seasons.SyncSeason
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.Episode
import net.simonvt.cathode.entitymapper.EpisodeListMapper
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class SeasonViewModel @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val syncSeason: SyncSeason
) : RefreshableViewModel() {

  private var seasonId = -1L

  lateinit var episodes: LiveData<List<Episode>>
    private set

  fun setSeasonId(seasonId: Long) {
    if (this.seasonId == -1L) {
      this.seasonId = seasonId

      episodes = MappedCursorLiveData(
        context,
        Episodes.fromSeason(seasonId),
        PROJECTION,
        null,
        null,
        EpisodeColumns.EPISODE + " ASC",
        EpisodeListMapper()
      )
    }
  }

  override suspend fun onRefresh() {
    val showId = seasonHelper.getShowId(seasonId)
    val traktId = showHelper.getTraktId(showId)
    val season = seasonHelper.getNumber(seasonId)
    syncSeason.invokeSync(SyncSeason.Params(traktId, season))
  }

  companion object {

    val PROJECTION = arrayOf(
      EpisodeColumns.ID,
      EpisodeColumns.TITLE,
      EpisodeColumns.SEASON,
      EpisodeColumns.EPISODE,
      EpisodeColumns.WATCHED,
      EpisodeColumns.IN_COLLECTION,
      EpisodeColumns.FIRST_AIRED,
      EpisodeColumns.SHOW_TITLE,
      LastModifiedColumns.LAST_MODIFIED
    )
  }
}
