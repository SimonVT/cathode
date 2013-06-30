package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.util.LogWrapper;

import java.util.List;

import javax.inject.Inject;

public class SyncMoviesTask extends TraktTask {

    private static final String TAG = "SyncMoviesTask";

    @Inject transient UserService mUserService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            queueTask(new SyncMoviesTask());

            List<Movie> movies = mUserService.moviesAll(DetailLevel.MIN);
            for (Movie movie : movies) {
                final Long tmdbId = movie.getTmdbId();
                final long movieId = MovieWrapper.getMovieId(mService.getContentResolver(), tmdbId);
                if (movieId == -1) {
                    queueTask(new SyncMovieTask(tmdbId));
                } else {
                    MovieWrapper.setIsInCollection(mService.getContentResolver(), movieId, movie.isInCollection());
                    MovieWrapper.setWatched(mService.getContentResolver(), movieId, movie.isWatched());
                }
            }

            queueTask(new SyncMoviesWatchlistTask());

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
