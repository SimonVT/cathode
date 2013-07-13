package net.simonvt.trakt.remote.sync;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Episode;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktDatabase;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class SyncEpisodeWatchlistTask extends TraktTask {

    private static final String TAG = "SyncEpisodeWatchlistTask";

    @Inject transient UserService mUserService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            Cursor c = mService.getContentResolver().query(TraktContract.Episodes.WATCHLIST_URI, new String[] {
                    TraktDatabase.Tables.EPISODES + "." + TraktContract.Episodes._ID,
            }, null, null, null);

            List<Long> episodeIds = new ArrayList<Long>();

            while (c.moveToNext()) {
                episodeIds.add(c.getLong(c.getColumnIndex(TraktContract.Episodes._ID)));
            }

            List<TvShow> shows = mUserService.watchlistEpisodes();

            for (TvShow show : shows) {
                final int tvdbId = show.getTvdbId();
                final long showId = ShowWrapper.getShowId(mService.getContentResolver(), tvdbId);

                if (showId != -1) {
                    List<Episode> episodes = show.getEpisodes();
                    for (Episode episode : episodes) {
                        final long episodeId = EpisodeWrapper.getEpisodeId(mService.getContentResolver(), episode);
                        EpisodeWrapper.setIsInWatchlist(mService.getContentResolver(), episodeId, true);
                        episodeIds.remove(episodeId);
                    }
                } else {
                    queueTask(new SyncShowTask(tvdbId));
                }
            }

            for (Long episodeId : episodeIds) {
                EpisodeWrapper.setIsInWatchlist(mService.getContentResolver(), episodeId, false);
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            postOnFailure();
            e.printStackTrace();
        }
    }
}
