package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.MovieService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.util.LogWrapper;

import javax.inject.Inject;

public class SyncMovieTask extends TraktTask {

    private static final String TAG = "SyncMovieTask";

    @Inject MovieService mMovieService;

    private long mTmdbId;

    public SyncMovieTask(long tmdbId) {
        mTmdbId = tmdbId;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            Movie movie = mMovieService.summary(mTmdbId, DetailLevel.EXTENDED);
            MovieWrapper.updateOrInsertMovie(mService.getContentResolver(), movie);

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
