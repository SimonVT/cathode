package net.simonvt.trakt.remote.sync;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.ServerTime;
import net.simonvt.trakt.api.entity.UpdatedShows;
import net.simonvt.trakt.api.service.ServerService;
import net.simonvt.trakt.api.service.ShowsService;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.remote.TraktTask;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

import javax.inject.Inject;

public class SyncUpdatedShows extends TraktTask {

    private static final String TAG = "SyncUpdatedShows";

    @Inject transient ShowsService mShowsService;

    @Inject transient ServerService mServerService;

    @Override
    protected void doTask() {
        try {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mService);
            final long showsLastUpdated = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0);

            long current;

            if (showsLastUpdated > 0) {
                UpdatedShows updatedShows = mShowsService.updated(showsLastUpdated);
                current = updatedShows.getTimestamps().getCurrent();

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
            } else {
                queueTask(new SyncShowsTask());
                ServerTime time = mServerService.time();
                current = time.getTimestamp();
            }

            settings.edit().putLong(Settings.SHOWS_LAST_UPDATED, current).apply();

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
