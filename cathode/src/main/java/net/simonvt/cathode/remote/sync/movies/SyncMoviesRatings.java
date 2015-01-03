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
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.RatingItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobFailedException;
import timber.log.Timber;

public class SyncMoviesRatings extends Job {

  @Inject transient SyncService syncService;

  @Override public String key() {
    return "SyncMoviesRatings";
  }

  @Override public int getPriority() {
    return PRIORITY_2;
  }

  @Override public void perform() {
    List<RatingItem> ratings = syncService.getMovieRatings();

    Cursor movies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.RATED_AT + ">0", null, null);
    List<Long> movieIds = new ArrayList<Long>();
    while (movies.moveToNext()) {
      final long movieId = movies.getLong(movies.getColumnIndex(MovieColumns.ID));
      movieIds.add(movieId);
    }
    movies.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    for (RatingItem rating : ratings) {
      final long traktId = rating.getMovie().getIds().getTrakt();
      long movieId = MovieWrapper.getMovieId(getContentResolver(), traktId);
      if (movieId == -1L) {
        movieId = MovieWrapper.createMovie(getContentResolver(), traktId);
        queue(new SyncMovie(traktId));
      }

      movieIds.remove(movieId);

      ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
          .withValue(MovieColumns.USER_RATING, rating.getRating())
          .withValue(MovieColumns.RATED_AT, rating.getRatedAt().getTimeInMillis())
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

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Unable to sync movie ratings");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "Unable to sync movie ratings");
      throw new JobFailedException(e);
    }
  }
}
