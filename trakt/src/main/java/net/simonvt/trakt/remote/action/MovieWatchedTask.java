package net.simonvt.trakt.remote.action;

import javax.inject.Inject;
import net.simonvt.trakt.api.body.MoviesBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.MovieService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
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
