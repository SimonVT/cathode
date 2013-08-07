package net.simonvt.trakt.remote.action;

import javax.inject.Inject;
import net.simonvt.trakt.api.body.RateBody;
import net.simonvt.trakt.api.service.RateService;
import net.simonvt.trakt.remote.TraktTask;
import retrofit.RetrofitError;

public class EpisodeRateTask extends TraktTask {

  private static final String TAG = "MovieRateTask";

  @Inject transient RateService rateService;

  private long tvdbId;

  private int episode;

  private int rating;

  public EpisodeRateTask(long tvdbId, int episode, int rating) {
    this.tvdbId = tvdbId;
    this.episode = episode;
    this.rating = rating;
  }

  @Override
  protected void doTask() {
    try {
      rateService.rateEpisode(
          new RateBody.Builder().tvdbId(tvdbId).episode(episode).rating(rating).build());
      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
