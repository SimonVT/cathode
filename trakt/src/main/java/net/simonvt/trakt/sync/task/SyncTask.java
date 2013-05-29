package net.simonvt.trakt.sync.task;

import net.simonvt.trakt.api.service.ShowsService;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.util.LogWrapper;

import javax.inject.Inject;

public class SyncTask extends TraktTask {

    private static final String TAG = "SyncTask";

    @Inject transient UserService mUserService;

    @Inject transient ShowsService mShowsService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        queueTask(new SyncShowsTask());
        queueTask(new SyncMoviesTask());

        postOnSuccess();
    }
}
