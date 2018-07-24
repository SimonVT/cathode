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

package net.simonvt.cathode.ui.movie;

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies;
import net.simonvt.cathode.ui.movies.MoviesAdapter;

public class RelatedMoviesViewModel extends AndroidViewModel {

  private long movieId = -1L;

  private LiveData<Cursor> movies;

  public RelatedMoviesViewModel(@NonNull Application application) {
    super(application);
  }

  public void setMovieId(long movieId) {
    if (this.movieId == -1L) {
      this.movieId = movieId;
      movies = new CursorLiveData(getApplication(), RelatedMovies.fromMovie(movieId),
          MoviesAdapter.PROJECTION, null, null, null);
    }
  }

  public LiveData<Cursor> getMovies() {
    return movies;
  }
}
