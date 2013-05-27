package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.util.LogWrapper;

import java.util.List;

import javax.inject.Inject;

public class FullSyncTask extends TraktTask {

    private static final String TAG = "FullSyncTask";

    @Inject transient UserService mUserService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        queueTask(new SyncShowsWatchlistTask());
        queueTask(new SyncEpisodeWatchlist());

        try {
            List<TvShow> shows = mUserService.libraryShowsAll(DetailLevel.MIN);

            for (TvShow show : shows) {
                final Integer tvdbId = show.getTvdbId();
                queueTask(new SyncShowTask(tvdbId));
            }

            queueTask(new SyncShowsWatchlistTask());
            queueTask(new SyncShowsCollectionTask());
            queueTask(new SyncEpisodeWatchlist());

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
