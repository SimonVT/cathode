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
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.entitymapper.MovieMapper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;

public class MovieHistoryViewModel extends AndroidViewModel {

  private long movieId = -1L;

  private SyncService syncService;
  private MovieDatabaseHelper movieHelper;

  private LiveData<Movie> movie;
  private MovieHistoryLiveData history;

  public MovieHistoryViewModel(@NonNull Application application, SyncService syncService,
      MovieDatabaseHelper movieHelper) {
    super(application);
    this.syncService = syncService;
    this.movieHelper = movieHelper;
  }

  public void setMovieId(long movieId) {
    if (this.movieId == -1L) {
      this.movieId = movieId;

      movie = new MappedCursorLiveData<>(getApplication(), Movies.withId(movieId),
          MovieHistoryFragment.MOVIE_PROJECTION, null, null, null, new MovieMapper());
      history = new MovieHistoryLiveData(movieId, syncService, movieHelper);
    }
  }

  public LiveData<Movie> getMovie() {
    return movie;
  }

  public MovieHistoryLiveData getHistory() {
    return history;
  }
}
