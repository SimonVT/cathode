package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.RateBody;
import net.simonvt.trakt.api.service.RateService;

import javax.inject.Inject;

public class EpisodeRateTask extends TraktTask {

    private static final String TAG = "MovieRateTask";

    @Inject transient RateService mRateService;

    private long mTvdbId;

    private int mEpisode;

    private int mRating;

    public EpisodeRateTask(long tvdbId, int episode, int rating) {
        mTvdbId = tvdbId;
        mEpisode = episode;
        mRating = rating;
    }

    @Override
    protected void doTask() {
        try {
            mRateService.rateEpisode(new RateBody.Builder().tvdbId(mTvdbId).episode(mEpisode).rating(mRating).build());
            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
