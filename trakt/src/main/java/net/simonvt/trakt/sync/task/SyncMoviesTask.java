package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.entity.UpdatedMovies;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.MoviesService;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.util.LogWrapper;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

import javax.inject.Inject;

public class SyncMoviesTask extends TraktTask {

    private static final String TAG = "SyncMoviesTask";

    @Inject transient UserService mUserService;

    @Inject transient MoviesService mMoviesService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            List<Movie> movies = mUserService.moviesAll(DetailLevel.MIN);
            for (Movie movie : movies) {
                final Long tmdbId = movie.getTmdbId();
                if (!MovieWrapper.exists(mService.getContentResolver(), tmdbId)) {
                    queueTask(new SyncMovieTask(tmdbId));
                }
            }

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mService);
            final long lastUpdated = settings.getLong(Settings.MOVIES_LAST_UPDATED, 0);

            UpdatedMovies updatedMovies = mMoviesService.updated(lastUpdated);

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
                    .putLong(Settings.SHOWS_LAST_UPDATED, updatedMovies.getTimestamps().getCurrent())
                    .apply();

            postOnSuccess();

        } catch (RetrofitError e) {
            e.printStackTrace();
            postOnFailure();
        }
    }
}
