package net.simonvt.trakt.remote.action;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.MoviesBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.MovieService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;

import javax.inject.Inject;

public class MovieCollectionTask extends TraktTask {

    private static final String TAG = "MovieCollectionTask";

    @Inject transient MovieService mMovieService;

    private final long mTmdbID;

    private final boolean mWatched;

    public MovieCollectionTask(long tmdbId, boolean watched) {
        if (tmdbId == 0) {
            // TODO
            throw new IllegalArgumentException("tvdb is 0");
        }
        mTmdbID = tmdbId;
        mWatched = watched;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            if (mWatched) {
                TraktResponse response = mMovieService.library(new MoviesBody(mTmdbID));
            } else {
                TraktResponse response = mMovieService.unlibrary(new MoviesBody(mTmdbID));
            }

            MovieWrapper.setIsInCollection(mService.getContentResolver(), mTmdbID, mWatched);

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
