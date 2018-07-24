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

package net.simonvt.cathode.ui.show;

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;

public class EpisodeHistoryViewModel extends AndroidViewModel {

  private long episodeId = -1L;

  private SyncService syncService;
  private EpisodeDatabaseHelper episodeHelper;

  private LiveData<Cursor> episode;
  private EpisodeHistoryLiveData history;

  public EpisodeHistoryViewModel(@NonNull Application application, SyncService syncService,
      EpisodeDatabaseHelper episodeHelper) {
    super(application);
    this.syncService = syncService;
    this.episodeHelper = episodeHelper;
  }

  public void setEpisodeId(long episodeId) {
    if (this.episodeId == -1L) {
      this.episodeId = episodeId;

      episode = new CursorLiveData(getApplication(), Episodes.withId(episodeId),
          EpisodeHistoryFragment.EPISODE_PROJECTION, null, null, null);
      history = new EpisodeHistoryLiveData(episodeId, syncService, episodeHelper);
    }
  }

  public LiveData<Cursor> getEpisode() {
    return episode;
  }

  public EpisodeHistoryLiveData getHistory() {
    return history;
  }
}
