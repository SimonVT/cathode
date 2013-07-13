package net.simonvt.trakt.remote.action;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.RateBody;
import net.simonvt.trakt.api.service.RateService;
import net.simonvt.trakt.remote.TraktTask;

import javax.inject.Inject;

public class MovieRateTask extends TraktTask {

    private static final String TAG = "MovieRateTask";

    @Inject transient RateService mRateService;

    private long mTmdbId;

    private int mRating;

    public MovieRateTask(long tmdbId, int rating) {
        mTmdbId = tmdbId;
        mRating = rating;
    }

    @Override
    protected void doTask() {
        try {
            mRateService.rateMovie(new RateBody.Builder().tmdbId(mTmdbId).rating(mRating).build());
            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
