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
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public class HiddenViewModel extends AndroidViewModel {

  private LiveData<Cursor> showsCalendar;
  private LiveData<Cursor> showsWatched;
  private LiveData<Cursor> showsCollected;
  private LiveData<Cursor> moviesCalendar;

  public HiddenViewModel(@NonNull Application application) {
    super(application);

    showsCalendar = new CursorLiveData(application, Shows.SHOWS, HiddenItemsAdapter.PROJECTION_SHOW,
        ShowColumns.HIDDEN_CALENDAR + "=1", null, Shows.SORT_TITLE);
    showsWatched = new CursorLiveData(application, Shows.SHOWS, HiddenItemsAdapter.PROJECTION_SHOW,
        ShowColumns.HIDDEN_WATCHED + "=1", null, Shows.SORT_TITLE);
    showsCollected =
        new CursorLiveData(application, Shows.SHOWS, HiddenItemsAdapter.PROJECTION_SHOW,
            ShowColumns.HIDDEN_COLLECTED + "=1", null, Shows.SORT_TITLE);
    moviesCalendar =
        new CursorLiveData(application, Movies.MOVIES, HiddenItemsAdapter.PROJECTION_MOVIES,
            MovieColumns.HIDDEN_CALENDAR + "=1", null, Movies.SORT_TITLE);
  }

  public LiveData<Cursor> getShowsCalendar() {
    return showsCalendar;
  }

  public LiveData<Cursor> getShowsWatched() {
    return showsWatched;
  }

  public LiveData<Cursor> getShowsCollected() {
    return showsCollected;
  }

  public LiveData<Cursor> getMoviesCalendar() {
    return moviesCalendar;
  }
}
