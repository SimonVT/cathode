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
package net.simonvt.cathode.remote.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncMovieRecommendations extends TraktTask {

  @Inject transient RecommendationsService recommendationsService;

  @Override protected void doTask() {
    try {
      ContentResolver resolver = service.getContentResolver();

      List<Long> movieIds = new ArrayList<Long>();
      Cursor c = resolver.query(CathodeContract.Movies.RECOMMENDED, null, null, null, null);
      while (c.moveToNext()) {
        movieIds.add(c.getLong(c.getColumnIndex(CathodeContract.Movies._ID)));
      }
      c.close();

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      List<Movie> recommendations = recommendationsService.movies();
      for (int index = 0; index < Math.min(recommendations.size(), 25); index++) {
        Movie movie = recommendations.get(index);
        long id = MovieWrapper.getMovieId(resolver, movie.getTmdbId());
        if (id == -1L) {
          queueTask(new SyncMovieTask(movie.getTmdbId()));
          id = MovieWrapper.insertMovie(resolver, movie);
        }

        movieIds.remove(id);

        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Movies.RECOMMENDATION_INDEX, index);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Movies.buildFromId(id))
                .withValues(cv)
                .build();
        ops.add(op);
      }

      for (Long id : movieIds) {
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Movies.buildFromId(id))
                .withValue(CathodeContract.Movies.RECOMMENDATION_INDEX, -1)
                .build();
        ops.add(op);
      }

      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);
      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    } catch (RemoteException e) {
      e.printStackTrace();
      postOnFailure();
    } catch (OperationApplicationException e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
