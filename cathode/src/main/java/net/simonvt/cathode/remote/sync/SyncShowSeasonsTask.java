package net.simonvt.cathode.remote.sync;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncShowSeasonsTask extends TraktTask {

  private static final String TAG = "SyncShowSeasonsTask";

  @Inject transient ShowService showService;

  private int tvdbId;

  public SyncShowSeasonsTask(int tvdbId) {
    this.tvdbId = tvdbId;
  }

  @Override
  protected void doTask() {
    try {
      final long showId = ShowWrapper.getShowId(service.getContentResolver(), tvdbId);

      List<Season> seasons = showService.seasons(tvdbId);

      for (Season season : seasons) {
        LogWrapper.v(TAG, "Scheduling sync for season " + season.getSeason() + " of " + tvdbId);
        SeasonWrapper.updateOrInsertSeason(service.getContentResolver(), season, showId);
        queueTask(new SyncSeasonTask(tvdbId, season.getSeason()));
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
