package net.simonvt.cathode.remote.sync;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeDatabase;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;

public class SyncEpisodeWatchlistTask extends TraktTask {

  private static final String TAG = "SyncEpisodeWatchlistTask";

  @Inject transient UserService userService;

  @Override protected void doTask() {
    try {
      Cursor c =
          service.getContentResolver().query(CathodeContract.Episodes.WATCHLIST_URI, new String[] {
              CathodeDatabase.Tables.EPISODES + "." + CathodeContract.Episodes._ID,
          }, null, null, null);

      List<Long> episodeIds = new ArrayList<Long>();

      while (c.moveToNext()) {
        episodeIds.add(c.getLong(c.getColumnIndex(CathodeContract.Episodes._ID)));
      }
      c.close();

      List<TvShow> shows = userService.watchlistEpisodes();

      for (TvShow show : shows) {
        final int tvdbId = show.getTvdbId();
        final long showId = ShowWrapper.getShowId(service.getContentResolver(), tvdbId);

        if (showId != -1) {
          List<Episode> episodes = show.getEpisodes();
          for (Episode episode : episodes) {
            final long episodeId =
                EpisodeWrapper.getEpisodeId(service.getContentResolver(), episode);
            EpisodeWrapper.setIsInWatchlist(service.getContentResolver(), episodeId, true);
            episodeIds.remove(episodeId);
          }
        } else {
          queueTask(new SyncShowTask(tvdbId));
        }
      }

      for (Long episodeId : episodeIds) {
        EpisodeWrapper.setIsInWatchlist(service.getContentResolver(), episodeId, false);
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      postOnFailure();
      e.printStackTrace();
    }
  }
}
