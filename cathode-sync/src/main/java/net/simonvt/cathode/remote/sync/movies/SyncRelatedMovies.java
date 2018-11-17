/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

import android.content.ContentProviderOperation;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.RelatedMoviesColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;

public class SyncRelatedMovies extends CallJob<List<Movie>> {

  private static final int RELATED_COUNT = 50;

  @Inject transient MoviesService moviesService;

  @Inject transient MovieDatabaseHelper movieHelper;

  private long traktId;

  public SyncRelatedMovies(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncRelatedMovies" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return JobPriority.EXTRAS;
  }

  @Override public Call<List<Movie>> getCall() {
    return moviesService.getRelated(traktId, RELATED_COUNT, Extended.FULL);
  }

  @Override public boolean handleResponse(List<Movie> movies) {
    final long movieId = movieHelper.getId(traktId);

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    List<Long> relatedIds = new ArrayList<>();

    Cursor related = getContentResolver().query(RelatedMovies.fromMovie(movieId), new String[] {
        Tables.MOVIE_RELATED + "." + RelatedMoviesColumns.ID,
    }, null, null, null);
    while (related.moveToNext()) {
      final long relatedMovieId = Cursors.getLong(related, RelatedMoviesColumns.ID);
      relatedIds.add(relatedMovieId);
    }
    related.close();

    int relatedIndex = 0;
    for (Movie movie : movies) {
      final long relatedMovieId = movieHelper.partialUpdate(movie);

      ContentProviderOperation op = ContentProviderOperation.newInsert(RelatedMovies.RELATED)
          .withValue(RelatedMoviesColumns.MOVIE_ID, movieId)
          .withValue(RelatedMoviesColumns.RELATED_MOVIE_ID, relatedMovieId)
          .withValue(RelatedMoviesColumns.RELATED_INDEX, relatedIndex)
          .build();
      ops.add(op);

      relatedIndex++;
    }

    for (Long id : relatedIds) {
      ops.add(ContentProviderOperation.newDelete(RelatedMovies.withId(id)).build());
    }

    return applyBatch(ops);
  }
}
