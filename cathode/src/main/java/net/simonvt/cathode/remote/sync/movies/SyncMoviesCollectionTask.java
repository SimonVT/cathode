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
import android.util.LongSparseArray;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.CollectionItem;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.TraktTask;

public class SyncMoviesCollectionTask extends TraktTask {

  @Inject transient SyncService syncService;

  @Override protected void doTask() {
    Cursor c = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID, MovieColumns.LISTED_AT,
    }, MovieColumns.IN_COLLECTION, null, null);

    LongSparseArray<Long> ids = new LongSparseArray<Long>();
    while (c.moveToNext()) {
      final long id = c.getLong(c.getColumnIndex(MovieColumns.ID));
      final long listedAt = c.getLong(c.getColumnIndex(MovieColumns.LISTED_AT));
      ids.put(id, listedAt);
    }
    c.close();

    List<CollectionItem> movies = syncService.getMovieCollection();

    for (CollectionItem item : movies) {
      Movie movie = item.getMovie();
      final long traktId = movie.getIds().getTrakt();
      long movieId = MovieWrapper.getMovieId(getContentResolver(), traktId);
      final long collectedAt = item.getCollectedAt().getTimeInMillis();

      if (movieId == -1) {
        queueTask(new SyncMovieTask(traktId));
        movieId = MovieWrapper.updateOrInsertMovie(getContentResolver(), movie);
      }

      Long lastListedAt = ids.get(movieId);
      if (lastListedAt != null) {
        ids.remove(movieId);
      }
      if (lastListedAt == null || lastListedAt != collectedAt) {
        MovieWrapper.setIsInCollection(getContentResolver(), movieId, true, collectedAt);
      }
    }

    final int size = ids.size();

    for (int i = 0; i < size; i++) {
      final long id = ids.keyAt(i);
      MovieWrapper.setIsInCollection(getContentResolver(), id, false);
    }

    postOnSuccess();
  }
}
