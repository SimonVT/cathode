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
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.ProviderSchematic;

public class SeasonViewModel extends AndroidViewModel {

  private long seasonId = -1L;

  private LiveData<Cursor> episodes;

  public SeasonViewModel(@NonNull Application application) {
    super(application);
  }

  public void setSeasonId(long seasonId) {
    if (this.seasonId == -1L) {
      this.seasonId = seasonId;

      episodes =
          new CursorLiveData(getApplication(), ProviderSchematic.Episodes.fromSeason(seasonId),
              SeasonAdapter.PROJECTION, null, null,
              DatabaseContract.EpisodeColumns.EPISODE + " ASC");
    }
  }

  public LiveData<Cursor> getEpisodes() {
    return episodes;
  }
}
