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

package net.simonvt.cathode.ui.movies.watchlist;

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.ui.movies.MoviesAdapter;

public class MovieWatchlistViewModel extends AndroidViewModel {

  private CursorLiveData movies;

  public MovieWatchlistViewModel(@NonNull Application application) {
    super(application);
    movies =
        new CursorLiveData(application, Movies.MOVIES_WATCHLIST, MoviesAdapter.PROJECTION, null,
            null, Movies.DEFAULT_SORT);
  }

  public LiveData<Cursor> getMovies() {
    return movies;
  }
}
