package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.entity.UpdatedShows;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.ShowsService;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.util.LogWrapper;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class QuickSyncTask extends TraktTask {

    private static final String TAG = "QuickSyncTask";

    @Inject transient UserService mUserService;

    @Inject transient ShowsService mShowsService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        Cursor c = mService.getContentResolver().query(TraktContract.Shows.CONTENT_URI, new String[] {
                TraktContract.Shows.TVDB_ID,
        }, null, null, null);

        final int tvdbIndex = c.getColumnIndex(TraktContract.Shows.TVDB_ID);

        List<Integer> tvdbIds = new ArrayList<Integer>();
        while (c.moveToNext()) {
            tvdbIds.add(c.getInt(tvdbIndex));
        }

        try {
            List<TvShow> shows = mUserService.libraryShowsAll(DetailLevel.MIN);

            for (TvShow show : shows) {
                final Integer tvdbId = show.getTvdbId();
                tvdbIds.remove(tvdbId);
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

            // Trakt returns the value in seconds
            settings.edit()
                    .putLong(Settings.SHOWS_LAST_UPDATED, updatedShows.getTimestamps().getCurrent() * 1000L)
                    .apply();

            for (int tvdbId : tvdbIds) {
                ShowWrapper.remove(mService.getContentResolver(), tvdbId);
            }

            queueTask(new SyncWatchedStatusTask());
            queueTask(new SyncShowsCollectionTask());
            queueTask(new SyncEpisodeWatchlist());
            queueTask(new SyncShowsWatchlistTask());

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
