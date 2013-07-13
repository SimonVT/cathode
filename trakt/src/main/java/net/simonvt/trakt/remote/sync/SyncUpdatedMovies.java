package net.simonvt.trakt.remote.sync;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.UpdatedMovies;
import net.simonvt.trakt.api.service.MoviesService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.remote.TraktTask;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

import javax.inject.Inject;

public class SyncUpdatedMovies extends TraktTask {

    private static final String TAG = "SyncUpdatedMovies";

    @Inject transient MoviesService mMoviesService;

    @Override
    protected void doTask() {
        try {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mService);
            final long moviesLastUpdated = settings.getLong(Settings.MOVIES_LAST_UPDATED, 0);

            UpdatedMovies updatedMovies = mMoviesService.updated(moviesLastUpdated);

            List<UpdatedMovies.MovieTimestamp> timestamps = updatedMovies.getMovies();
            for (UpdatedMovies.MovieTimestamp timestamp : timestamps) {
                final int tmdbId = timestamp.getTmdbId();
                final boolean exists = MovieWrapper.exists(mService.getContentResolver(), tmdbId);
                if (exists) {
                    final boolean needsUpdate = MovieWrapper.needsUpdate(mService.getContentResolver(),
                            tmdbId, timestamp.getLastUpdated());
                    if (needsUpdate) {
                        queueTask(new SyncMovieTask(tmdbId));
                    }
                }
            }

            settings.edit()
                    .putLong(Settings.MOVIES_LAST_UPDATED, updatedMovies.getTimestamps().getCurrent())
                    .apply();

            postOnSuccess();
        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
