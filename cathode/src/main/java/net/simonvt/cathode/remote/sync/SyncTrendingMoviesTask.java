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
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncTrendingMoviesTask extends TraktTask {

  @Inject transient MoviesService moviesService;

  @Override protected void doTask() {
    try {
      ContentResolver resolver = service.getContentResolver();

      List<Movie> movies = moviesService.trending();

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      Cursor c = resolver.query(CathodeContract.Movies.TRENDING, null, null, null, null);
      while (c.moveToNext()) {
        final long movieId = c.getLong(c.getColumnIndex(CathodeContract.Movies._ID));
        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Movies.TRENDING_INDEX, -1);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Movies.buildMovieUri(movieId))
                .withValues(cv)
                .build();
        ops.add(op);
      }

      for (int i = 0, count = Math.min(movies.size(), 25); i < count; i++) {
        Movie movie = movies.get(i);
        long movieId = MovieWrapper.getMovieId(resolver, movie.getTmdbId());
        if (movieId == -1L) {
          queueTask(new SyncMovieTask(movie.getTmdbId()));
          movieId = MovieWrapper.insertMovie(resolver, movie);
        }

        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Movies.TRENDING_INDEX, i);
        ContentProviderOperation op =
            ContentProviderOperation.newUpdate(CathodeContract.Movies.buildMovieUri(movieId))
                .withValues(cv)
                .build();
        ops.add(op);
      }

      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);
      postOnSuccess();
    } catch (RetrofitError error) {
      error.printStackTrace();
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
