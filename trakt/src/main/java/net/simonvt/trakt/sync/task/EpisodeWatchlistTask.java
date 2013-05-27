package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.ShowEpisodeBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.util.LogWrapper;

import javax.inject.Inject;

public class EpisodeWatchlistTask extends TraktTask {

    private static final String TAG = "EpisodeWatchlistTask";

    @Inject transient ShowService mShowService;

    private final int mTvdbId;

    private final int mSeason;

    private final int mEpisode;

    private final boolean mInWatchlist;

    public EpisodeWatchlistTask(int tvdbId, int season, int episode, boolean inWatchlist) {
        mTvdbId = tvdbId;
        mSeason = season;
        mEpisode = episode;
        mInWatchlist = inWatchlist;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            if (mInWatchlist) {
                TraktResponse response = mShowService.episodeWatchlist(new ShowEpisodeBody(mTvdbId, mSeason, mEpisode));
            } else {
                TraktResponse response =
                        mShowService.episodeUnwatchlist(new ShowEpisodeBody(mTvdbId, mSeason, mEpisode));
            }

            EpisodeWrapper.setIsInWatchlist(mService.getContentResolver(), mTvdbId, mSeason, mEpisode, mInWatchlist);

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
