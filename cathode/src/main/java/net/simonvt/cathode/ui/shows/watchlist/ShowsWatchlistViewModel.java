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

package net.simonvt.cathode.ui.shows.watchlist;

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public class ShowsWatchlistViewModel extends AndroidViewModel {

  private LiveData<Cursor> shows;
  private LiveData<Cursor> episodes;

  public ShowsWatchlistViewModel(@NonNull Application application) {
    super(application);
    shows =
        new CursorLiveData(application, Shows.SHOWS_WATCHLIST, ShowWatchlistAdapter.PROJECTION_SHOW,
            null, null, Shows.DEFAULT_SORT);
    episodes = new CursorLiveData(application, Episodes.EPISODES_IN_WATCHLIST,
        ShowWatchlistAdapter.PROJECTION_EPISODE, null, null, EpisodeColumns.SHOW_ID + " ASC");
  }

  public LiveData<Cursor> getShows() {
    return shows;
  }

  public LiveData<Cursor> getEpisodes() {
    return episodes;
  }
}
