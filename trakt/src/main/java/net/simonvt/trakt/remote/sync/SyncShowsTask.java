package net.simonvt.trakt.remote.sync;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncShowsTask extends TraktTask {

  private static final String TAG = "SyncShowsTask";

  @Inject transient UserService userService;

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      List<TvShow> shows = userService.libraryShowsAll(DetailLevel.MIN);

      for (TvShow show : shows) {
        final Integer tvdbId = show.getTvdbId();
        if (!ShowWrapper.exists(service.getContentResolver(), tvdbId)) {
          queueTask(new SyncShowTask(tvdbId));
        }
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
