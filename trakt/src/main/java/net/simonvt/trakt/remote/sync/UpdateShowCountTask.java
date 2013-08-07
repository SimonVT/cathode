package net.simonvt.trakt.remote.sync;

import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;

public class UpdateShowCountTask extends TraktTask {

  private static final String TAG = "UpdateShowCountTask";

  private long showId;

  public UpdateShowCountTask(long showId) {
    this.showId = showId;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");
    ShowWrapper.updateShowCounts(service.getContentResolver(), showId);
    postOnSuccess();
  }
}
