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

package net.simonvt.cathode.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.entity.ShowWithEpisode;
import net.simonvt.cathode.entitymapper.MovieMapper;
import net.simonvt.cathode.entitymapper.ShowWithEpisodeMapper;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public class HomeViewModel extends AndroidViewModel {

  private LiveData<ShowWithEpisode> watchingShow;
  private LiveData<Movie> watchingMovie;

  public HomeViewModel(@NonNull Application application) {
    super(application);
    watchingShow = new MappedCursorLiveData<>(application, Shows.SHOW_WATCHING,
        ShowWithEpisodeMapper.PROJECTION, null, null, null, new ShowWithEpisodeMapper());
    watchingMovie =
        new MappedCursorLiveData<>(application, ProviderSchematic.Movies.WATCHING, null, null, null,
            null, new MovieMapper());
  }

  public LiveData<ShowWithEpisode> getWatchingShow() {
    return watchingShow;
  }

  public LiveData<Movie> getWatchingMovie() {
    return watchingMovie;
  }
}
