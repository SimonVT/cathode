package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.util.LogWrapper;

import java.util.List;

import javax.inject.Inject;

public class SyncShowsTask extends TraktTask {

    private static final String TAG = "SyncShowsTask";

    @Inject transient UserService mUserService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            List<TvShow> shows = mUserService.libraryShowsAll(DetailLevel.MIN);

            for (TvShow show : shows) {
                final Integer tvdbId = show.getTvdbId();
                if (!ShowWrapper.exists(mService.getContentResolver(), tvdbId)) {
                    queueTask(new SyncShowTask(tvdbId));
                }
            }

            queueTask(new SyncWatchedStatusTask());
            queueTask(new SyncEpisodeWatchlist());
            queueTask(new SyncShowsWatchlistTask());
            queueTask(new SyncShowsCollectionTask());

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
