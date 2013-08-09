package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.ShowsBody;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class ShowWatchlistTask extends TraktTask {

  private static final String TAG = "ShowWatchlistTask";

  @Inject ShowService showService;

  private int tvdbId;

  private boolean inWatchlist;

  public ShowWatchlistTask(int tvdbId, boolean inWatchlist) {
    this.tvdbId = tvdbId;
    this.inWatchlist = inWatchlist;
  }

  @Override
  protected void doTask() {
    try {
      if (inWatchlist) {
        showService.watchlist(new ShowsBody(tvdbId));
      } else {
        showService.unwatchlist(new ShowsBody(tvdbId));
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
