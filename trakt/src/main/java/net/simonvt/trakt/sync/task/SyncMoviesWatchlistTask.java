package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.provider.TraktContract;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class SyncMoviesWatchlistTask extends TraktTask {

    private static final String TAG = "SyncMoviesWatchlistTask";

    @Inject UserService mUserService;

    @Override
    protected void doTask() {
        try {
            Cursor c = mService.getContentResolver().query(TraktContract.Movies.CONTENT_URI, new String[] {
                    TraktContract.Movies._ID,
            }, TraktContract.Movies.IN_WATCHLIST, null, null);

            List<Long> movieIds = new ArrayList<Long>();

            while (c.moveToNext()) {
                movieIds.add(c.getLong(c.getColumnIndex(TraktContract.Movies._ID)));
            }

            List<Movie> movies = mUserService.watchlistMovies();

            for (Movie movie : movies) {
                final long tmdbId = movie.getTmdbId();
                final long movieId = MovieWrapper.getMovieId(mService.getContentResolver(), tmdbId);

                if (movieId != -1) {
                    MovieWrapper.setIsInWatchlist(mService.getContentResolver(), movieId, true);
                    movieIds.remove(movieId);
                } else {
                    queueTask(new SyncMovieTask(tmdbId));
                }
            }

            for (Long movieId : movieIds) {
                MovieWrapper.setIsInWatchlist(mService.getContentResolver(), movieId, false);
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
