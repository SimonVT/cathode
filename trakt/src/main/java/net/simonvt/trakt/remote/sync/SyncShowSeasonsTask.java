package net.simonvt.trakt.remote.sync;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Season;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;

import java.util.List;

import javax.inject.Inject;

public class SyncShowSeasonsTask extends TraktTask {

    private static final String TAG = "SyncShowSeasonsTask";

    @Inject transient ShowService mShowService;

    private int mTvdbId;

    public SyncShowSeasonsTask(int tvdbId) {
        mTvdbId = tvdbId;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            final long showId = ShowWrapper.getShowId(mService.getContentResolver(), mTvdbId);

            List<Season> seasons = mShowService.seasons(mTvdbId);

            for (Season season : seasons) {
                LogWrapper.v(TAG, "Scheduling sync for season " + season.getSeason() + " of " + mTvdbId);
                SeasonWrapper.updateOrInsertSeason(mService.getContentResolver(), season, showId);
                queueTask(new SyncSeasonTask(mTvdbId, season.getSeason()));
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
