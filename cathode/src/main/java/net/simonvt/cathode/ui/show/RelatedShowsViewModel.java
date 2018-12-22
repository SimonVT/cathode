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
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.entitymapper.ShowListMapper;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows;
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter;

public class RelatedShowsViewModel extends AndroidViewModel {

  private long showId = -1L;

  private LiveData<List<Show>> shows;

  public RelatedShowsViewModel(@NonNull Application application) {
    super(application);
  }

  public void setShowId(long showId) {
    if (this.showId == -1L) {
      this.showId = showId;
      shows = new MappedCursorLiveData<>(getApplication(), RelatedShows.fromShow(showId),
          ShowDescriptionAdapter.PROJECTION, null, null, null, new ShowListMapper());
    }
  }

  public LiveData<List<Show>> getShows() {
    return shows;
  }
}
