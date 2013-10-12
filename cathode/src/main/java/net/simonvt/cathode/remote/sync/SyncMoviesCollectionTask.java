package net.simonvt.cathode.remote.sync;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncMoviesCollectionTask extends TraktTask {

  private static final String TAG = "SyncMoviesCollectionTask";

  @Inject transient UserService userService;

  @Override
  protected void doTask() {
    try {
      Cursor c =
          service.getContentResolver().query(CathodeContract.Movies.CONTENT_URI, new String[] {
              CathodeContract.Movies._ID,
          }, CathodeContract.Movies.IN_COLLECTION, null, null);

      List<Long> movieIds = new ArrayList<Long>(c.getCount());

      while (c.moveToNext()) {
        movieIds.add(c.getLong(0));
      }
      c.close();

      List<Movie> movies = userService.moviesCollection(DetailLevel.MIN);

      for (Movie movie : movies) {
        final Long tmdbId = movie.getTmdbId();
        final long movieId = MovieWrapper.getMovieId(service.getContentResolver(), tmdbId);

        if (movieId == -1) {
          queueTask(new SyncMovieTask(tmdbId));
        } else {
          if (!movieIds.remove(movieId)) {
            MovieWrapper.setIsInCollection(service.getContentResolver(), movieId, true);
          }
        }
      }

      for (Long movieId : movieIds) {
        MovieWrapper.setIsInCollection(service.getContentResolver(), movieId, false);
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
