package net.simonvt.trakt.remote.action;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.body.ShowBody;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.remote.TraktTask;

import android.database.Cursor;

import javax.inject.Inject;

public class ShowWatchedTask extends TraktTask {

    private static final String TAG = "ShowWatchedTask";

    @Inject ShowService mShowService;

    private int mTvdbId;

    private boolean mWatched;

    public ShowWatchedTask(int tvdbId, boolean watched) {
        mTvdbId = tvdbId;
        mWatched = watched;
    }

    @Override
    protected void doTask() {
        try {
            if (mWatched) {
                mShowService.seen(new ShowBody(mTvdbId));
            } else {
                // Trakt doesn't expose an unseen api..
                final long showId = ShowWrapper.getShowId(mService.getContentResolver(), mTvdbId);
                Cursor c = mService.getContentResolver()
                        .query(TraktContract.Episodes.buildFromShowId(showId), new String[] {
                                TraktContract.Episodes.SEASON,
                                TraktContract.Episodes.EPISODE,
                        }, null, null, null);

                while (c.moveToNext()) {
                    queuePriorityTask(new EpisodeWatchedTask(mTvdbId, c.getInt(0), c.getInt(0), false));
                }
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
