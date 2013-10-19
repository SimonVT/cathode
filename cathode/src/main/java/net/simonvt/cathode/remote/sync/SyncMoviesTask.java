package net.simonvt.cathode.remote.sync;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncMoviesTask extends TraktTask {

  private static final String TAG = "SyncMoviesTask";

  @Inject transient UserService userService;

  @Override
  protected void doTask() {
    try {
      List<Movie> movies = userService.moviesAll(DetailLevel.MIN);
      for (Movie movie : movies) {
        final long tmdbId = movie.getTmdbId();
        queueTask(new SyncMovieTask(tmdbId));
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
