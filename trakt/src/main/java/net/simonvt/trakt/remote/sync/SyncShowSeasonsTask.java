package net.simonvt.trakt.remote.sync;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.api.entity.Season;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
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
    LogWrapper.v(TAG, "[doTask]");

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
