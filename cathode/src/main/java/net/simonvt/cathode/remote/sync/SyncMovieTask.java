package net.simonvt.cathode.remote.sync;

import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.MovieService;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncMovieTask extends TraktTask {

  private static final String TAG = "SyncMovieTask";

  @Inject transient MovieService movieService;

  private long tmdbId;

  public SyncMovieTask(long tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override protected void doTask() {
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
