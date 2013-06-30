package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.LastActivity;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.ActivityWrapper;
import net.simonvt.trakt.util.LogWrapper;

import javax.inject.Inject;

public class SyncTask extends TraktTask {

    private static final String TAG = "SyncTask";

    @Inject transient UserService mUserService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            queueTask(new SyncUpdatedShows());
            queueTask(new SyncUpdatedMovies());

            LastActivity lastActivity = mUserService.lastActivity();

            LastActivity.ActivityItem episodeActivity = lastActivity.getEpisode();
            Long lastWatched = episodeActivity.getWatched();
            Long lastCollected = episodeActivity.getCollection();

            if (lastWatched == null || lastWatched == 0 || lastCollected == null || lastCollected == 0) {
                queueTask(new SyncShowsTask());
            } else {
                boolean needsUpdate =
                        ActivityWrapper.episodeWatchedNeedsUpdate(mService.getContentResolver(), lastWatched);
                if (needsUpdate) {
                    queueTask(new SyncWatchedStatusTask());
                }
                needsUpdate = ActivityWrapper.episodeCollectedNeedsUpdate(mService.getContentResolver(), lastWatched);
                if (needsUpdate) {
                    queueTask(new SyncShowsCollectionTask());
                }
            }

            LastActivity.ActivityItem movieActivity = lastActivity.getMovie();
            lastWatched = movieActivity.getWatched();
            lastCollected = movieActivity.getCollection();

            if (lastWatched == null || lastWatched == 0 || lastCollected == null || lastCollected == 0) {
                queueTask(new SyncMoviesTask());
            } else {
                boolean needsUpdate =
                        ActivityWrapper.movieWatchedNeedsUpdate(mService.getContentResolver(), lastWatched);
                needsUpdate |= ActivityWrapper.movieCollectedNeedsUpdate(mService.getContentResolver(), lastWatched);
                if (needsUpdate) {
                    queueTask(new SyncMoviesTask());
                }
            }

            ActivityWrapper.update(mService.getContentResolver(), lastActivity);

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
