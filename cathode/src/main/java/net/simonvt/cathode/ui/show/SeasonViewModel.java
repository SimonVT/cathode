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
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.entitymapper.EpisodeListMapper;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;

public class SeasonViewModel extends AndroidViewModel {

  public static final String[] PROJECTION = {
      EpisodeColumns.ID, EpisodeColumns.TITLE, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
      EpisodeColumns.WATCHED, EpisodeColumns.IN_COLLECTION, EpisodeColumns.FIRST_AIRED,
      EpisodeColumns.SHOW_TITLE, LastModifiedColumns.LAST_MODIFIED,
  };

  private long seasonId = -1L;

  private LiveData<List<Episode>> episodes;

  public SeasonViewModel(@NonNull Application application) {
    super(application);
  }

  public void setSeasonId(long seasonId) {
    if (this.seasonId == -1L) {
      this.seasonId = seasonId;

      episodes =
          new MappedCursorLiveData<>(getApplication(), Episodes.fromSeason(seasonId), PROJECTION,
              null, null, EpisodeColumns.EPISODE + " ASC", new EpisodeListMapper());
    }
  }

  public LiveData<List<Episode>> getEpisodes() {
    return episodes;
  }
}
