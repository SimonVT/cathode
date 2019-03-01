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

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class SyncMovieRecommendations extends CallJob<List<Movie>> {

  private static final int LIMIT = 50;

  @Inject transient RecommendationsService recommendationsService;

  @Inject transient MovieDatabaseHelper movieHelper;

  public SyncMovieRecommendations() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncMovieRecommendations";
  }

  @Override public int getPriority() {
    return JobPriority.SUGGESTIONS;
  }

  @Override public Call<List<Movie>> getCall() {
    return recommendationsService.movies(LIMIT, Extended.FULL);
  }

  @Override public boolean handleResponse(List<Movie> recommendations) {
    List<Long> movieIds = new ArrayList<>();
    Cursor c = getContentResolver().query(Movies.RECOMMENDED, null, null, null, null);
    while (c.moveToNext()) {
      movieIds.add(Cursors.getLong(c, MovieColumns.ID));
    }
    c.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    for (int index = 0, count = recommendations.size(); index < count; index++) {
      Movie movie = recommendations.get(index);

      final long movieId = movieHelper.partialUpdate(movie);

      movieIds.remove(movieId);

      ContentValues values = new ContentValues();
      values.put(MovieColumns.RECOMMENDATION_INDEX, index);
      ContentProviderOperation op =
          ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(values).build();
      ops.add(op);
    }

    for (Long id : movieIds) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(id))
          .withValue(MovieColumns.RECOMMENDATION_INDEX, -1)
          .build();
      ops.add(op);
    }

    return applyBatch(ops);
  }
}
