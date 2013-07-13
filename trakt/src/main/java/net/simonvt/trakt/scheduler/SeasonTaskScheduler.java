package net.simonvt.trakt.scheduler;

import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.remote.action.EpisodeCollectionTask;

import android.content.Context;
import android.database.Cursor;

import javax.inject.Inject;

public class SeasonTaskScheduler extends BaseTaskScheduler {

    @Inject EpisodeTaskScheduler mEpisodeScheduler;

    public SeasonTaskScheduler(Context context) {
        super(context);
    }

    public void setWatched(final long seasonId, final boolean watched) {
        execute(new Runnable() {
            @Override
            public void run() {
                Cursor c = mContext.getContentResolver()
                        .query(TraktContract.Episodes.buildFromSeasonId(seasonId), new String[] {
                                TraktContract.Episodes._ID,
                                TraktContract.Episodes.WATCHED,
                        }, null, null, null);

                while (c.moveToNext()) {
                    final long episodeId = c.getLong(c.getColumnIndex(TraktContract.Episodes._ID));
                    final boolean episodeWatched = c.getLong(c.getColumnIndex(TraktContract.Episodes.WATCHED)) != 0;

                    if (watched != episodeWatched) {
                        mEpisodeScheduler.setWatched(episodeId, watched);
                    }
                }
            }
        });
    }

    public void setInCollection(final long seasonId, final boolean inCollection) {
        execute(new Runnable() {
            @Override
            public void run() {
                Cursor c = mContext.getContentResolver()
                        .query(TraktContract.Episodes.buildFromSeasonId(seasonId), new String[] {
                                TraktContract.Episodes.SHOW_ID,
                                TraktContract.Episodes.SEASON,
                                TraktContract.Episodes.EPISODE,
                                TraktContract.Episodes.IN_COLLECTION,
                        }, null, null, null);

                while (c.moveToNext()) {
                    final long showId = c.getLong(c.getColumnIndex(TraktContract.Episodes.SHOW_ID));
                    final int tvdbId = ShowWrapper.getTvdbId(mContext.getContentResolver(), showId);
                    final int season = c.getInt(c.getColumnIndex(TraktContract.Episodes.SEASON));
                    final int number = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));
                    final boolean episodeInCollection =
                            c.getLong(c.getColumnIndex(TraktContract.Episodes.IN_COLLECTION)) != 0;

                    if (inCollection != episodeInCollection) {
                        postPriorityTask(new EpisodeCollectionTask(tvdbId, season, number, inCollection));
                    }
                }
            }
        });
    }
}
