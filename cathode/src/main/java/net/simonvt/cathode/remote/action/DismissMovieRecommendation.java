package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.DismissBody;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class DismissMovieRecommendation extends TraktTask {

  @Inject transient RecommendationsService recommendationsService;

  private long tmdbId;

  public DismissMovieRecommendation(long tmdbId) {
    this.tmdbId = tmdbId;
  }

  @Override protected void doTask() {
    try {
      recommendationsService.dismissMovie(new DismissBody().tmdbId(tmdbId));

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
