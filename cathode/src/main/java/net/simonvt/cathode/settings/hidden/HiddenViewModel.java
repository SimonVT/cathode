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

package net.simonvt.cathode.settings.hidden;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.entitymapper.MovieListMapper;
import net.simonvt.cathode.entitymapper.ShowListMapper;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public class HiddenViewModel extends AndroidViewModel {

  private LiveData<List<Show>> showsCalendar;
  private LiveData<List<Show>> showsWatched;
  private LiveData<List<Show>> showsCollected;
  private LiveData<List<Movie>> moviesCalendar;

  public HiddenViewModel(@NonNull Application application) {
    super(application);

    showsCalendar =
        new MappedCursorLiveData<>(application, Shows.SHOWS, HiddenItemsAdapter.PROJECTION_SHOW,
            ShowColumns.HIDDEN_CALENDAR + "=1", null, Shows.SORT_TITLE, new ShowListMapper());
    showsWatched =
        new MappedCursorLiveData<>(application, Shows.SHOWS, HiddenItemsAdapter.PROJECTION_SHOW,
            ShowColumns.HIDDEN_WATCHED + "=1", null, Shows.SORT_TITLE, new ShowListMapper());
    showsCollected =
        new MappedCursorLiveData<>(application, Shows.SHOWS, HiddenItemsAdapter.PROJECTION_SHOW,
            ShowColumns.HIDDEN_COLLECTED + "=1", null, Shows.SORT_TITLE, new ShowListMapper());
    moviesCalendar =
        new MappedCursorLiveData<>(application, Movies.MOVIES, HiddenItemsAdapter.PROJECTION_MOVIES,
            MovieColumns.HIDDEN_CALENDAR + "=1", null, Movies.SORT_TITLE, new MovieListMapper());
  }

  public LiveData<List<Show>> getShowsCalendar() {
    return showsCalendar;
  }

  public LiveData<List<Show>> getShowsWatched() {
    return showsWatched;
  }

  public LiveData<List<Show>> getShowsCollected() {
    return showsCollected;
  }

  public LiveData<List<Movie>> getMoviesCalendar() {
    return moviesCalendar;
  }
}
