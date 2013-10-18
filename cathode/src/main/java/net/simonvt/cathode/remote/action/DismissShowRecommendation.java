package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.DismissBody;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class DismissShowRecommendation extends TraktTask {

  @Inject transient RecommendationsService recommendationsService;

  private int tvdbId;

  public DismissShowRecommendation(int tvdbId) {
    this.tvdbId = tvdbId;
  }

  @Override protected void doTask() {
    try {
      recommendationsService.dismissShow(new DismissBody().tvdbId(tvdbId));

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
