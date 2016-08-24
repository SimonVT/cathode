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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import timber.log.Timber;

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
    return PRIORITY_SUGGESTIONS;
  }

  @Override public Call<List<Movie>> getCall() {
    return recommendationsService.movies(LIMIT, Extended.FULL_IMAGES);
  }

  @Override public void handleResponse(List<Movie> recommendations) {
    try {
      ContentResolver resolver = getContentResolver();

      List<Long> movieIds = new ArrayList<>();
      Cursor c = resolver.query(Movies.RECOMMENDED, null, null, null, null);
      while (c.moveToNext()) {
        movieIds.add(Cursors.getLong(c, MovieColumns.ID));
      }
      c.close();

      ArrayList<ContentProviderOperation> ops = new ArrayList<>();
      for (int index = 0, count = recommendations.size(); index < count; index++) {
        Movie movie = recommendations.get(index);
        final long movieId = movieHelper.partialUpdate(movie);

        movieIds.remove(movieId);

        ContentValues cv = new ContentValues();
        cv.put(MovieColumns.RECOMMENDATION_INDEX, index);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(cv).build();
        ops.add(op);
      }

      for (Long id : movieIds) {
        ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(id))
            .withValue(MovieColumns.RECOMMENDATION_INDEX, -1)
            .build();
        ops.add(op);
      }

      resolver.applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Unable to update recommendations");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "Unable to update recommendations");
      throw new JobFailedException(e);
    }
  }
}
