package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.ShowEpisodeBody;
import net.simonvt.cathode.api.entity.TraktResponse;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.LogWrapper;
import retrofit.RetrofitError;

public class EpisodeWatchedTask extends TraktTask {

  private static final String TAG = "EpisodeWatchedTask";

  @Inject transient ShowService showService;

  private final int tvdbId;

  private final int season;

  private final int episode;

  private final boolean watched;

  public EpisodeWatchedTask(int tvdbId, int season, int episode, boolean watched) {
    if (tvdbId == 0) {
      // TODO
      throw new IllegalArgumentException("tvdb is 0");
    }
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
    this.watched = watched;
  }

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      if (watched) {
        TraktResponse response =
            showService.episodeSeen(new ShowEpisodeBody(tvdbId, season, episode));
      } else {
        TraktResponse response =
            showService.episodeUnseen(new ShowEpisodeBody(tvdbId, season, episode));
      }

      EpisodeWrapper.setWatched(service.getContentResolver(), tvdbId, season, episode, watched);

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
