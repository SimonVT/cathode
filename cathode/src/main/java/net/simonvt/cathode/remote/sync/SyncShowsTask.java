package net.simonvt.cathode.remote.sync;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
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
