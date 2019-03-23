/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
import androidx.work.WorkManager;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.RatingItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class SyncMoviesRatings extends CallJob<List<RatingItem>> {

  @Inject transient WorkManager workManager;

  @Inject transient SyncService syncService;

  @Inject transient MovieDatabaseHelper movieHelper;

  public SyncMoviesRatings() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncMoviesRatings";
  }

  @Override public int getPriority() {
    return JobPriority.EXTRAS;
  }

  @Override public Call<List<RatingItem>> getCall() {
    return syncService.getMovieRatings();
  }

  @Override public boolean handleResponse(List<RatingItem> ratings) {
    Cursor movies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.RATED_AT + ">0", null, null);
    List<Long> movieIds = new ArrayList<>();
    while (movies.moveToNext()) {
      final long movieId = Cursors.getLong(movies, MovieColumns.ID);
      movieIds.add(movieId);
    }
    movies.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (RatingItem rating : ratings) {
      final long traktId = rating.getMovie().getIds().getTrakt();
      MovieDatabaseHelper.IdResult result = movieHelper.getIdOrCreate(traktId);
      final long movieId = result.movieId;

      movieIds.remove(movieId);

      ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
          .withValue(MovieColumns.USER_RATING, rating.getRating())
          .withValue(MovieColumns.RATED_AT, rating.getRated_at().getTimeInMillis())
          .build();
      ops.add(op);
    }

    for (Long movieId : movieIds) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
          .withValue(MovieColumns.USER_RATING, 0)
          .withValue(MovieColumns.RATED_AT, 0)
          .build();
      ops.add(op);
    }

    return applyBatch(ops);
  }
}
