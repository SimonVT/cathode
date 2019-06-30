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
import androidx.lifecycle.ViewModel
import net.simonvt.cathode.api.service.SyncService
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entitymapper.EpisodeMapper
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import javax.inject.Inject

class EpisodeHistoryViewModel @Inject constructor(
  private val context: Context,
  private val syncService: SyncService,
  private val episodeHelper: EpisodeDatabaseHelper
) : ViewModel() {

  private var episodeId = -1L

  var episode: LiveData<Episode>? = null
    private set
  var history: EpisodeHistoryLiveData? = null
    private set

  fun setEpisodeId(episodeId: Long) {
    if (this.episodeId == -1L) {
      this.episodeId = episodeId

      episode = MappedCursorLiveData(
        context,
        Episodes.withId(episodeId),
        EpisodeMapper.projection,
        mapper = EpisodeMapper
      )
      history = EpisodeHistoryLiveData(episodeId, syncService, episodeHelper)
    }
  }
}
