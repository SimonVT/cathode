package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.ShowBody;
import net.simonvt.trakt.api.service.ShowService;

import javax.inject.Inject;

public class ShowCollectionTask extends TraktTask {

    private static final String TAG = "ShowWatchlistTask";

    @Inject ShowService mShowService;

    private int mTvdbId;

    private boolean mInWatchlist;

    public ShowCollectionTask(int tvdbId, boolean inWatchlist) {
        mTvdbId = tvdbId;
        mInWatchlist = inWatchlist;
    }

    @Override
    protected void doTask() {
        try {
            if (mInWatchlist) {
                mShowService.library(new ShowBody(mTvdbId));
            } else {
                mShowService.unlibrary(new ShowBody(mTvdbId));
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
