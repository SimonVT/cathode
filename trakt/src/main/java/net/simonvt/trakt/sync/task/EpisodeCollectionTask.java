package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.ShowEpisodeBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.util.LogWrapper;

import javax.inject.Inject;

public class EpisodeCollectionTask extends TraktTask {

    private static final String TAG = "EpisodeCollectionTask";

    @Inject transient ShowService mShowService;

    private final int mTvdbId;

    private final int mSeason;

    private final int mEpisode;

    private final boolean mInCollection;

    public EpisodeCollectionTask(int tvdbId, int season, int episode, boolean inCollection) {
        mTvdbId = tvdbId;
        mSeason = season;
        mEpisode = episode;
        mInCollection = inCollection;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            if (mInCollection) {
                TraktResponse response = mShowService.episodeLibrary(new ShowEpisodeBody(mTvdbId, mSeason, mEpisode));
            } else {
                TraktResponse response = mShowService.episodeUnlibrary(new ShowEpisodeBody(mTvdbId, mSeason, mEpisode));
            }

            EpisodeWrapper.setInCollection(mService.getContentResolver(), mTvdbId, mSeason, mEpisode, mInCollection);

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
