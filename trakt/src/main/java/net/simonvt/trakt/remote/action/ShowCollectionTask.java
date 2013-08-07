package net.simonvt.trakt.remote.action;

import javax.inject.Inject;
import net.simonvt.trakt.api.body.ShowBody;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.remote.TraktTask;
import retrofit.RetrofitError;

public class ShowCollectionTask extends TraktTask {

  private static final String TAG = "ShowWatchlistTask";

  @Inject ShowService showService;

  private int tvdbId;

  private boolean inWatchlist;

  public ShowCollectionTask(int tvdbId, boolean inWatchlist) {
    this.tvdbId = tvdbId;
    this.inWatchlist = inWatchlist;
  }

  @Override
  protected void doTask() {
    try {
      if (inWatchlist) {
        showService.library(new ShowBody(tvdbId));
      } else {
        showService.unlibrary(new ShowBody(tvdbId));
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
