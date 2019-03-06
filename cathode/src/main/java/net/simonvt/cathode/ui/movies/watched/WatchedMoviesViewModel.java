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

package net.simonvt.cathode.ui.movies.watched;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.entitymapper.MovieListMapper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.movies.MoviesAdapter;
import net.simonvt.cathode.ui.movies.watched.WatchedMoviesFragment.SortBy;

public class WatchedMoviesViewModel extends AndroidViewModel {

  private MappedCursorLiveData<List<Movie>> movies;

  private SortBy sortBy;

  public WatchedMoviesViewModel(@NonNull Application application) {
    super(application);
    sortBy = SortBy.fromValue(
        Settings.get(application).getString(Settings.Sort.MOVIE_WATCHED, SortBy.TITLE.getKey()));
    movies =
        new MappedCursorLiveData<>(application, Movies.MOVIES_WATCHED, MoviesAdapter.PROJECTION,
            null, null, sortBy.getSortOrder(), new MovieListMapper());
  }

  public void setSortBy(SortBy sortBy) {
    this.sortBy = sortBy;
    movies.setSortOrder(sortBy.getSortOrder());
  }

  public LiveData<List<Movie>> getMovies() {
    return movies;
  }
}