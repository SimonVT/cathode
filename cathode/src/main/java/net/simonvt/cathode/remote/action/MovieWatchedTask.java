package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.MoviesBody;
import net.simonvt.cathode.api.entity.TraktResponse;
import net.simonvt.cathode.api.service.MovieService;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
import retrofit.RetrofitError;

public class MovieWatchedTask extends TraktTask {

  private static final String TAG = "MovieWatchedTask";

  @Inject transient MovieService movieService;

  private final long tmdbId;

  private final boolean watched;

  public MovieWatchedTask(long tmdbId, boolean watched) {
    if (tmdbId == 0) {
      // TODO
      throw new IllegalArgumentException("tvdb is 0");
    }
    this.tmdbId = tmdbId;
    this.watched = watched;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      if (watched) {
        TraktResponse response = movieService.seen(new MoviesBody(tmdbId));
      } else {
        TraktResponse response = movieService.unseen(new MoviesBody(tmdbId));
      }

      MovieWrapper.setWatched(service.getContentResolver(), tmdbId, watched);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
