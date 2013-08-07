package net.simonvt.trakt.remote.sync;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.api.entity.Episode;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktDatabase;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncEpisodeWatchlistTask extends TraktTask {

  private static final String TAG = "SyncEpisodeWatchlistTask";

  @Inject transient UserService userService;

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      Cursor c =
          service.getContentResolver().query(TraktContract.Episodes.WATCHLIST_URI, new String[] {
              TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes._ID,
          }, null, null, null);

      List<Long> episodeIds = new ArrayList<Long>();

      while (c.moveToNext()) {
        episodeIds.add(c.getLong(c.getColumnIndex(TraktContract.Episodes._ID)));
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
