package net.simonvt.trakt.remote.action;

import javax.inject.Inject;
import net.simonvt.trakt.api.body.RateBody;
import net.simonvt.trakt.api.service.RateService;
import net.simonvt.trakt.remote.TraktTask;
import retrofit.RetrofitError;

public class ShowRateTask extends TraktTask {

  private static final String TAG = "MovieRateTask";

  @Inject transient RateService rateService;

  private long tvdbId;

  private int rating;

  public ShowRateTask(long tvdbId, int rating) {
    this.tvdbId = tvdbId;
    this.rating = rating;
  }

  @Override
  protected void doTask() {
    try {
      rateService.rateShow(new RateBody.Builder().tvdbId(tvdbId).rating(rating).build());
      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
