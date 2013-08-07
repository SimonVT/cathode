package net.simonvt.trakt.remote.sync;

import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;

public class UpdateSeasonCountTask extends TraktTask {

  private static final String TAG = "UpdateSeasonCountTask";

  private long seasonId;

  public UpdateSeasonCountTask(long seasonId) {
    this.seasonId = seasonId;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");
    SeasonWrapper.updateSeasonCounts(service.getContentResolver(), seasonId);
    postOnSuccess();
  }
}
