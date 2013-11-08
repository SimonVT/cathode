package net.simonvt.cathode.remote.sync;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncShowsWatchlistTask extends TraktTask {

  private static final String TAG = "SyncShowsWatchlistTask";

  @Inject transient UserService userService;

  @Override protected void doTask() {
    try {
      Cursor c =
          service.getContentResolver().query(CathodeContract.Shows.SHOWS_WATCHLIST, new String[] {
              CathodeDatabase.Tables.SHOWS + "." + CathodeContract.Shows._ID,
          }, null, null, null);

      List<Long> showIds = new ArrayList<Long>();

      while (c.moveToNext()) {
        showIds.add(c.getLong(c.getColumnIndex(CathodeContract.Shows._ID)));
      }
      c.close();

      List<TvShow> shows = userService.watchlistShows();

      for (TvShow show : shows) {
        final int tvdbId = show.getTvdbId();
        final long showId = ShowWrapper.getShowId(service.getContentResolver(), tvdbId);

        if (showId != -1L) {
          if (!showIds.remove(showId)) {
            ShowWrapper.setIsInWatchlist(service.getContentResolver(), tvdbId, true);
          }
        } else {
          queueTask(new SyncShowTask(tvdbId));
        }
      }

      for (Long showId : showIds) {
        ShowWrapper.setIsInWatchlist(service.getContentResolver(), showId, false);
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      postOnFailure();
      e.printStackTrace();
    }
  }
}
