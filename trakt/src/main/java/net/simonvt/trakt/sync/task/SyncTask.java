package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.LastActivity;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.settings.ActivityWrapper;
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

            long episodeLastWatched = lastActivity.getEpisode().getWatched();
            long episodeLastCollected = lastActivity.getEpisode().getCollection();
            long episodeLastWatchlist = lastActivity.getEpisode().getWatchlist();

            long showLastWatchlist = lastActivity.getShow().getWatchlist();

            long movieLastWatched = lastActivity.getMovie().getWatched();
            long movieLastCollected = lastActivity.getMovie().getCollection();
            long movieLastWatchlist = lastActivity.getMovie().getWatchlist();

            if (ActivityWrapper.episodeWatchedNeedsUpdate(mService, episodeLastWatched)) {
                queueTask(new SyncShowsWatchedTask());
            }

            if (ActivityWrapper.episodeCollectedNeedsUpdate(mService, episodeLastCollected)) {
                queueTask(new SyncShowsCollectionTask());
            }

            if (ActivityWrapper.episodeWatchlistNeedsUpdate(mService, episodeLastWatchlist)) {
                queueTask(new SyncEpisodeWatchlistTask());
            }

            if (ActivityWrapper.showWatchlistNeedsUpdate(mService, showLastWatchlist)) {
                queueTask(new SyncShowsWatchlistTask());
            }

            if (ActivityWrapper.movieWatchedNeedsUpdate(mService, movieLastWatched)) {
                queueTask(new SyncMoviesWatchedTask());
            }

            if (ActivityWrapper.movieCollectedNeedsUpdate(mService, movieLastCollected)) {
                queueTask(new SyncMoviesCollectionTask());
            }

            if (ActivityWrapper.movieWatchlistNeedsUpdate(mService, movieLastWatchlist)) {
                queueTask(new SyncMoviesWatchlistTask());
            }

            ActivityWrapper.update(mService, lastActivity);

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
