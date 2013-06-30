package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.UpdatedShows;
import net.simonvt.trakt.api.service.ShowsService;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.settings.Settings;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

import javax.inject.Inject;

public class SyncUpdatedShows extends TraktTask {

    private static final String TAG = "SyncUpdatedShows";

    @Inject transient ShowsService mShowsService;

    @Override
    protected void doTask() {
        try {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mService);
            final long showsLastUpdated = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0);

            UpdatedShows updatedShows = mShowsService.updated(showsLastUpdated);

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

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
