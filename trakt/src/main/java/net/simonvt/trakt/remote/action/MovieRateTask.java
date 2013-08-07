package net.simonvt.trakt.remote.action;

import javax.inject.Inject;
import net.simonvt.trakt.api.body.RateBody;
import net.simonvt.trakt.api.service.RateService;
import net.simonvt.trakt.remote.TraktTask;
import retrofit.RetrofitError;

public class MovieRateTask extends TraktTask {

  private static final String TAG = "MovieRateTask";

  @Inject transient RateService rateService;

  private long tmdbId;

  private int rating;

  public MovieRateTask(long tmdbId, int rating) {
    this.tmdbId = tmdbId;
    this.rating = rating;
  }

  @Override
  protected void doTask() {
    try {
      rateService.rateMovie(new RateBody.Builder().tmdbId(tmdbId).rating(rating).build());
      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
