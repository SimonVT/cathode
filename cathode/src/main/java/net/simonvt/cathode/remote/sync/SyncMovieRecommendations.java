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
            ContentProviderOperation.newUpdate(CathodeContract.Movies.buildMovieUri(id))
                .withValues(cv)
                .build();
        ops.add(op);
      }

      for (Long id : movieIds) {
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Movies.buildMovieUri(id))
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
