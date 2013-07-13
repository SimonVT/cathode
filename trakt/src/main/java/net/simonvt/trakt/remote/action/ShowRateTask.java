package net.simonvt.trakt.remote.action;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.RateBody;
import net.simonvt.trakt.api.service.RateService;
import net.simonvt.trakt.remote.TraktTask;

import javax.inject.Inject;

public class ShowRateTask extends TraktTask {

    private static final String TAG = "MovieRateTask";

    @Inject transient RateService mRateService;

    private long mTvdbId;

    private int mRating;

    public ShowRateTask(long tvdbId, int rating) {
        mTvdbId = tvdbId;
        mRating = rating;
    }

    @Override
    protected void doTask() {
        try {
            mRateService.rateShow(new RateBody.Builder().tvdbId(mTvdbId).rating(mRating).build());
            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
