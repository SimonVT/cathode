package net.simonvt.cathode.remote.sync;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncMoviesWatchlistTask extends TraktTask {

  private static final String TAG = "SyncMoviesWatchlistTask";

  @Inject UserService userService;

  @Override
  protected void doTask() {
    try {
      Cursor c =
          service.getContentResolver().query(CathodeContract.Movies.CONTENT_URI, new String[] {
              CathodeContract.Movies._ID,
          }, CathodeContract.Movies.IN_WATCHLIST, null, null);

      List<Long> movieIds = new ArrayList<Long>();

      while (c.moveToNext()) {
        movieIds.add(c.getLong(c.getColumnIndex(CathodeContract.Movies._ID)));
      }
      c.close();

      List<Movie> movies = userService.watchlistMovies();

      for (Movie movie : movies) {
        final long tmdbId = movie.getTmdbId();
        final long movieId = MovieWrapper.getMovieId(service.getContentResolver(), tmdbId);

        if (movieId == -1) {
          queueTask(new SyncMovieTask(tmdbId));
        } else {
          if (!movieIds.remove(movieId)) {
            MovieWrapper.setIsInWatchlist(service.getContentResolver(), movieId, true);
          }
        }
      }

      for (Long movieId : movieIds) {
        MovieWrapper.setIsInWatchlist(service.getContentResolver(), movieId, false);
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
