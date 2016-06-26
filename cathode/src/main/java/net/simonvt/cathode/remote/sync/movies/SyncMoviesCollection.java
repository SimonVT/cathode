/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote.sync.movies;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.CollectionItem;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;

public class SyncMoviesCollection extends CallJob<List<CollectionItem>> {

  @Inject transient SyncService syncService;

  @Inject transient MovieDatabaseHelper movieHelper;

  public SyncMoviesCollection() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncMoviesCollection";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public Call<List<CollectionItem>> getCall() {
    return syncService.getMovieCollection();
  }

  @Override public void handleResponse(List<CollectionItem> movies) {
    Cursor c = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.IN_COLLECTION, null, null);

    List<Long> movieIds = new ArrayList<>();
    while (c.moveToNext()) {
      final long id = Cursors.getLong(c, MovieColumns.ID);
      movieIds.add(id);
    }
    c.close();

    for (CollectionItem item : movies) {
      Movie movie = item.getMovie();
      final long traktId = movie.getIds().getTrakt();
      MovieDatabaseHelper.IdResult result = movieHelper.getIdOrCreate(traktId);
      final long movieId = result.movieId;
      final long collectedAt = item.getCollectedAt().getTimeInMillis();

      if (!movieIds.remove(movieId)) {
        movieHelper.setIsInCollection(movieId, true, collectedAt);

        if (movieHelper.needsSync(movieId)) {
          queue(new SyncMovie(traktId));
        }
      }
    }

    final int size = movieIds.size();

    for (int i = 0; i < size; i++) {
      final long id = movieIds.get(i);
      movieHelper.setIsInCollection(id, false);
    }
  }
}
