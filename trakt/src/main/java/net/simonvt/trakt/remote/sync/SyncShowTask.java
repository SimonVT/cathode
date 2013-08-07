package net.simonvt.trakt.remote.sync;

import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.api.entity.Episode;
import net.simonvt.trakt.api.entity.Season;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncShowTask extends TraktTask {

  private static final String TAG = "SyncShowTask";

  @Inject transient ShowService showService;

  private final int tvdbId;

  public SyncShowTask(int tvdbId) {
    this.tvdbId = tvdbId;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      TvShow show = showService.summary(tvdbId, DetailLevel.EXTENDED);
      final long showId = ShowWrapper.updateOrInsertShow(service.getContentResolver(), show);

      List<Season> seasons = show.getSeasons();

      for (Season season : seasons) {
        final long seasonId =
            SeasonWrapper.updateOrInsertSeason(service.getContentResolver(), season, showId);
        List<Episode> episodes = season.getEpisodes().getEpisodes();
        for (Episode episode : episodes) {
          EpisodeWrapper.updateOrInsertEpisode(service.getContentResolver(), episode, showId,
              seasonId);
        }
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
