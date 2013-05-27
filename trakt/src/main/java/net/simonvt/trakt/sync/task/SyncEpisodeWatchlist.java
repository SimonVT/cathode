package net.simonvt.trakt.sync.task;

import retrofit.RetrofitError;

import net.simonvt.trakt.api.entity.Episode;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.util.LogWrapper;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class SyncEpisodeWatchlist extends TraktTask {

    private static final String TAG = "SyncEpisodeWatchlist";

    @Inject transient UserService mUserService;

    @Override
    protected void doTask() {
        LogWrapper.v(TAG, "[doTask]");

        try {
            List<TvShow> shows = mUserService.watchlistEpisodes();

            Cursor c = mService.getContentResolver().query(TraktContract.Episodes.CONTENT_URI, new String[] {
                    TraktContract.Episodes._ID,
            }, TraktContract.Episodes.IN_WATCHLIST + "=1", null, null);

            List<Long> episodeIds = new ArrayList<Long>();

            while (c.moveToNext()) {
                episodeIds.add(c.getLong(c.getColumnIndex(TraktContract.Episodes._ID)));
            }

            for (TvShow show : shows) {
                final int tvdbId = show.getTvdbId();

                // TODO: queueTask(new SyncShowTask(tvdbId));

                List<Episode> episodes = show.getEpisodes();

                for (Episode episode : episodes) {
                    final long episodeId = EpisodeWrapper.getEpisodeId(mService.getContentResolver(), episode);
                    if (episodeId != -1) {
                        EpisodeWrapper.setIsInWatchlist(mService.getContentResolver(), episodeId, true);
                        episodeIds.remove(episodeId);
                    }
                }
            }

            for (Long episodeId : episodeIds) {
                EpisodeWrapper.setIsInWatchlist(mService.getContentResolver(), episodeId, false);
            }

            postOnSuccess();

        } catch (RetrofitError e) {
            postOnFailure();
        }
    }
}
