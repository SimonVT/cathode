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

package net.simonvt.cathode.ui.suggestions.movies;

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.movies.MoviesAdapter;
import net.simonvt.cathode.ui.suggestions.movies.TrendingMoviesFragment.SortBy;

public class TrendingMoviesViewModel extends AndroidViewModel {

  private CursorLiveData trending;

  private SortBy sortBy;

  public TrendingMoviesViewModel(@NonNull Application application) {
    super(application);
    sortBy = SortBy.fromValue(
        Settings.get(application).getString(Settings.Sort.MOVIE_TRENDING, SortBy.VIEWERS.getKey()));
    trending =
        new CursorLiveData(application, Movies.TRENDING, MoviesAdapter.PROJECTION, null, null,
            sortBy.getSortOrder());
  }

  public void setSortBy(SortBy sortBy) {
    this.sortBy = sortBy;
    trending.setSortOrder(sortBy.getSortOrder());
  }

  public LiveData<Cursor> getTrending() {
    return trending;
  }
}
