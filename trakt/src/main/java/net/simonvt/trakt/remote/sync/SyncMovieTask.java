package net.simonvt.trakt.remote.sync;

import javax.inject.Inject;
import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.MovieService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncMovieTask extends TraktTask {

  private static final String TAG = "SyncMovieTask";

  @Inject MovieService movieService;

  private long tmdbId;

  public SyncMovieTask(long tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      Movie movie = movieService.summary(tmdbId, DetailLevel.EXTENDED);
      MovieWrapper.updateOrInsertMovie(service.getContentResolver(), movie);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
