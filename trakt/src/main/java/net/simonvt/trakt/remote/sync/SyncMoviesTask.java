package net.simonvt.trakt.remote.sync;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.remote.TraktTask;
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
            List<Movie> movies = mUserService.moviesAll(DetailLevel.MIN);
            for (Movie movie : movies) {
                final long tmdbId = movie.getTmdbId();
                queueTask(new SyncMovieTask(tmdbId));
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
