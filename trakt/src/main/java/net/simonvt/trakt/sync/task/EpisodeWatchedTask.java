package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.ShowEpisodeBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.util.LogWrapper;

import javax.inject.Inject;

public class EpisodeWatchedTask extends TraktTask {

    private static final String TAG = "EpisodeWatchedTask";

    @Inject transient ShowService mShowService;

    private final int mTvdbId;

    private final int mSeason;

    private final int mEpisode;

    private final boolean mWatched;

    public EpisodeWatchedTask(int tvdbId, int season, int episode, boolean watched) {
        if (tvdbId == 0) {
            // TODO
            throw new IllegalArgumentException("tvdb is 0");
        }
        mTvdbId = tvdbId;
        mSeason = season;
        mEpisode = episode;
        mWatched = watched;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            if (mWatched) {
                TraktResponse response = mShowService.episodeSeen(new ShowEpisodeBody(mTvdbId, mSeason, mEpisode));
            } else {
                TraktResponse response = mShowService.episodeUnseen(new ShowEpisodeBody(mTvdbId, mSeason, mEpisode));
            }

            EpisodeWrapper.setWatched(mService.getContentResolver(), mTvdbId, mSeason, mEpisode, mWatched);

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
