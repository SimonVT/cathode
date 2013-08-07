package net.simonvt.trakt.remote.action;

import javax.inject.Inject;
import net.simonvt.trakt.api.body.ShowsBody;
import net.simonvt.trakt.api.service.ShowService;
import net.simonvt.trakt.remote.TraktTask;
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
