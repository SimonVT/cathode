package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.util.LogWrapper;

import android.content.ContentResolver;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class SyncMoviesCollectionTask extends TraktTask {

    private static final String TAG = "SyncMoviesCollectionTask";

    @Inject transient UserService mUserService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            Cursor c = mService.getContentResolver().query(TraktContract.Movies.CONTENT_URI, new String[] {
                    TraktContract.Movies._ID,
            }, TraktContract.Movies.IN_COLLECTION, null, null);

            List<Long> movieIds = new ArrayList<Long>(c.getCount());

            while (c.moveToNext()) {
                movieIds.add(c.getLong(0));
            }

            List<Movie> movies = mUserService.moviesCollection(DetailLevel.MIN);

            for (Movie movie : movies) {
                final Long tmdbId = movie.getTmdbId();
                final long movieId = MovieWrapper.getMovieId(mService.getContentResolver(), tmdbId);

                if (movieId == -1) {
                    queueTask(new SyncMovieTask(tmdbId));
                } else {
                    MovieWrapper.setIsInCollection(mService.getContentResolver(), movieId, true);
                    movieIds.remove(movieId);
                }
            }

            for (Long movieId : movieIds) {
                MovieWrapper.setIsInCollection(mService.getContentResolver(), movieId, false);
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
