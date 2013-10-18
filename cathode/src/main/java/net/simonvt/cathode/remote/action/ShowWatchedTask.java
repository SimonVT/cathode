package net.simonvt.cathode.remote.action;

import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.ShowBody;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class ShowWatchedTask extends TraktTask {

  private static final String TAG = "ShowWatchedTask";

  @Inject transient ShowService showService;

  private int tvdbId;

  private boolean watched;

  public ShowWatchedTask(int tvdbId, boolean watched) {
    this.tvdbId = tvdbId;
    this.watched = watched;
  }

  @Override
  protected void doTask() {
    try {
      if (watched) {
        showService.seen(new ShowBody(tvdbId));
      } else {
        // Trakt doesn't expose an unseen api..
        final long showId = ShowWrapper.getShowId(service.getContentResolver(), tvdbId);
        Cursor c = service.getContentResolver()
            .query(CathodeContract.Episodes.buildFromShowId(showId), new String[] {
                CathodeContract.Episodes.SEASON, CathodeContract.Episodes.EPISODE,
            }, null, null, null);

        while (c.moveToNext()) {
          queuePriorityTask(new EpisodeWatchedTask(tvdbId, c.getInt(0), c.getInt(0), false));
        }

        c.close();
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
