package net.simonvt.trakt.sync.task;

import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.util.LogWrapper;

public class UpdateSeasonCountTask extends TraktTask {

    private static final String TAG = "UpdateSeasonCountTask";

    private long mSeasonId;

    public UpdateSeasonCountTask(long seasonId) {
        mSeasonId = seasonId;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");
        SeasonWrapper.updateSeasonCounts(mService.getContentResolver(), mSeasonId);
        postOnSuccess();
    }
}
