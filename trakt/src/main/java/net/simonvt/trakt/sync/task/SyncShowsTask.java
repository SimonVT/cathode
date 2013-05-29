package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.entity.UpdatedShows;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.ShowsService;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.util.LogWrapper;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

import javax.inject.Inject;

public class SyncShowsTask extends TraktTask {

    private static final String TAG = "SyncShowsTask";

    @Inject transient UserService mUserService;

    @Inject transient ShowsService mShowsService;

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

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mService);
            final long lastUpdated = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0);

            UpdatedShows updatedShows = mShowsService.updated(lastUpdated);

            List<UpdatedShows.ShowTimestamp> timestamps = updatedShows.getShows();
            for (UpdatedShows.ShowTimestamp timestamp : timestamps) {
                final int tvdbId = timestamp.getTvdbId();
                final boolean exists = ShowWrapper.exists(mService.getContentResolver(), tvdbId);
                if (exists) {
                    final boolean needsUpdate = ShowWrapper.needsUpdate(mService.getContentResolver(),
                            tvdbId, timestamp.getLastUpdated());
                    if (needsUpdate) {
                        queueTask(new SyncShowTask(tvdbId));
                    }
                }
            }

            settings.edit()
                    .putLong(Settings.SHOWS_LAST_UPDATED, updatedShows.getTimestamps().getCurrent())
                    .apply();

            queueTask(new SyncWatchedStatusTask());
            queueTask(new SyncEpisodeWatchlist());
            queueTask(new SyncShowsWatchlistTask());

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
