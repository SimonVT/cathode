package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.ShowsBody;
import net.simonvt.trakt.api.service.ShowService;

import javax.inject.Inject;

public class ShowWatchlistTask extends TraktTask {

    private static final String TAG = "ShowWatchlistTask";

    @Inject ShowService mShowService;

    private int mTvdbId;

    private boolean mInWatchlist;

    public ShowWatchlistTask(int tvdbId, boolean inWatchlist) {
        mTvdbId = tvdbId;
        mInWatchlist = inWatchlist;
    }

    @Override
    protected void doTask() {
        try {
            if (mInWatchlist) {
                mShowService.watchlist(new ShowsBody(mTvdbId));
            } else {
                mShowService.unwatchlist(new ShowsBody(mTvdbId));
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
