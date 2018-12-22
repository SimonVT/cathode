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
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.entitymapper.MovieListMapper;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies;
import net.simonvt.cathode.ui.movies.MoviesAdapter;

public class RelatedMoviesViewModel extends AndroidViewModel {

  private long movieId = -1L;

  private LiveData<List<Movie>> movies;

  public RelatedMoviesViewModel(@NonNull Application application) {
    super(application);
  }

  public void setMovieId(long movieId) {
    if (this.movieId == -1L) {
      this.movieId = movieId;
      movies = new MappedCursorLiveData<>(getApplication(), RelatedMovies.fromMovie(movieId),
          MoviesAdapter.PROJECTION, null, null, null, new MovieListMapper());
    }
  }

  public LiveData<List<Movie>> getMovies() {
    return movies;
  }
}
