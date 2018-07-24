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
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows;
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter;

public class RelatedShowsViewModel extends AndroidViewModel {

  private long showId = -1L;

  private LiveData<Cursor> shows;

  public RelatedShowsViewModel(@NonNull Application application) {
    super(application);
  }

  public void setShowId(long showId) {
    if (this.showId == -1L) {
      this.showId = showId;
      shows = new CursorLiveData(getApplication(), RelatedShows.fromShow(showId),
          ShowDescriptionAdapter.PROJECTION, null, null, null);
    }
  }

  public LiveData<Cursor> getShows() {
    return shows;
  }
}
