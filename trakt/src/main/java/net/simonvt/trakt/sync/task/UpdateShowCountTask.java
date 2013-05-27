package net.simonvt.trakt.sync.task;

import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.util.LogWrapper;

public class UpdateShowCountTask extends TraktTask {

    private static final String TAG = "UpdateShowCountTask";

    private long mShowId;

    public UpdateShowCountTask(long showId) {
        mShowId = showId;
    }

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");
        ShowWrapper.updateShowCounts(mService.getContentResolver(), mShowId);
        postOnSuccess();
    }
}
