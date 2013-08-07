package net.simonvt.trakt.remote.sync;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.api.entity.UpdatedMovies;
import net.simonvt.trakt.api.service.MoviesService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.settings.Settings;
import retrofit.RetrofitError;

public class SyncUpdatedMovies extends TraktTask {

  private static final String TAG = "SyncUpdatedMovies";

  @Inject transient MoviesService moviesService;

  @Override
  protected void doTask() {
    try {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(service);
      final long moviesLastUpdated = settings.getLong(Settings.MOVIES_LAST_UPDATED, 0);

      UpdatedMovies updatedMovies = moviesService.updated(moviesLastUpdated);

      List<UpdatedMovies.MovieTimestamp> timestamps = updatedMovies.getMovies();
      for (UpdatedMovies.MovieTimestamp timestamp : timestamps) {
        final int tmdbId = timestamp.getTmdbId();
        final boolean exists = MovieWrapper.exists(service.getContentResolver(), tmdbId);
        if (exists) {
          final boolean needsUpdate =
              MovieWrapper.needsUpdate(service.getContentResolver(), tmdbId,
                  timestamp.getLastUpdated());
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
