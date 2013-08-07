package net.simonvt.trakt.remote.action;

import javax.inject.Inject;
import net.simonvt.trakt.api.body.ShowEpisodeBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class EpisodeWatchlistTask extends TraktTask {

  private static final String TAG = "EpisodeWatchlistTask";

  @Inject transient ShowService showService;

  private final int tvdbId;

  private final int season;

  private final int episode;

  private final boolean inWatchlist;

  public EpisodeWatchlistTask(int tvdbId, int season, int episode, boolean inWatchlist) {
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
    this.inWatchlist = inWatchlist;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      if (inWatchlist) {
        TraktResponse response =
            showService.episodeWatchlist(new ShowEpisodeBody(tvdbId, season, episode));
      } else {
        TraktResponse response =
            showService.episodeUnwatchlist(new ShowEpisodeBody(tvdbId, season, episode));
      }

      EpisodeWrapper.setIsInWatchlist(service.getContentResolver(), tvdbId, season, episode,
          inWatchlist);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
