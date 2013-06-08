package net.simonvt.trakt.scheduler;

import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.sync.task.EpisodeCollectionTask;
import net.simonvt.trakt.sync.task.EpisodeRateTask;
import net.simonvt.trakt.sync.task.EpisodeWatchedTask;
import net.simonvt.trakt.sync.task.EpisodeWatchlistTask;
import net.simonvt.trakt.sync.task.SyncEpisodeTask;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class EpisodeTaskScheduler extends BaseTaskScheduler {

    private static final String TAG = "EpisodeTaskScheduler";

    public EpisodeTaskScheduler(Context context) {
        super(context);
    }

    /**
     * Sync data for episode with Trakt.
     *
     * @param episodeId The database id of the episode.
     */
    public void sync(final long episodeId) {
        execute(new Runnable() {
            @Override
            public void run() {
                Cursor c = EpisodeWrapper.query(mContext.getContentResolver(), episodeId,
                        TraktContract.Episodes.SHOW_ID,
                        TraktContract.Episodes.SEASON,
                        TraktContract.Episodes.EPISODE);
                c.moveToFirst();
                final long showId = c.getLong(c.getColumnIndex(TraktContract.Episodes.SHOW_ID));
                final int tvdbId = ShowWrapper.getTvdbId(mContext.getContentResolver(), showId);
                final int season = c.getInt(c.getColumnIndex(TraktContract.Episodes.SEASON));
                final int number = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));

                postTask(new SyncEpisodeTask(tvdbId, season, number));
            }
        });
    }

    /**
     * Add episodes watched outside of trakt to user library.
     *
     * @param episodeId The database id of the episode.
     * @param watched   Whether the episode has been watched.
     */
    public void setWatched(final long episodeId, final boolean watched) {
        execute(new Runnable() {
            @Override
            public void run() {
                Cursor c = EpisodeWrapper.query(mContext.getContentResolver(), episodeId,
                        TraktContract.Episodes.SHOW_ID,
                        TraktContract.Episodes.SEASON,
                        TraktContract.Episodes.EPISODE);
                c.moveToFirst();
                final long showId = c.getLong(c.getColumnIndex(TraktContract.Episodes.SHOW_ID));
                final int tvdbId = ShowWrapper.getTvdbId(mContext.getContentResolver(), showId);
                final int season = c.getInt(c.getColumnIndex(TraktContract.Episodes.SEASON));
                final int number = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));

                EpisodeWrapper.setWatched(mContext.getContentResolver(), episodeId, watched);

                postPriorityTask(new EpisodeWatchedTask(tvdbId, season, number, watched));
            }
        });
    }

    /**
     * Add episodes to user library collection.
     *
     * @param episodeId The database id of the episode.
     */
    public void setIsInCollection(final long episodeId, final boolean inCollection) {
        execute(new Runnable() {
            @Override
            public void run() {
                Cursor c = EpisodeWrapper.query(mContext.getContentResolver(), episodeId,
                        TraktContract.Episodes.SHOW_ID,
                        TraktContract.Episodes.SEASON,
                        TraktContract.Episodes.EPISODE);
                c.moveToFirst();
                final long showId = c.getLong(c.getColumnIndex(TraktContract.Episodes.SHOW_ID));
                final int tvdbId = ShowWrapper.getTvdbId(mContext.getContentResolver(), showId);
                final int season = c.getInt(c.getColumnIndex(TraktContract.Episodes.SEASON));
                final int number = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));

                EpisodeWrapper.setInCollection(mContext.getContentResolver(), episodeId, inCollection);

                postPriorityTask(new EpisodeCollectionTask(tvdbId, season, number, inCollection));
            }
        });
    }

    public void setIsInWatchlist(final long episodeId, final boolean inWatchlist) {
        execute(new Runnable() {
            @Override
            public void run() {
                Cursor c = EpisodeWrapper.query(mContext.getContentResolver(), episodeId,
                        TraktContract.Episodes.SHOW_ID,
                        TraktContract.Episodes.SEASON,
                        TraktContract.Episodes.EPISODE);
                c.moveToFirst();
                final long showId = c.getLong(c.getColumnIndex(TraktContract.Episodes.SHOW_ID));
                final int tvdbId = ShowWrapper.getTvdbId(mContext.getContentResolver(), showId);
                final int season = c.getInt(c.getColumnIndex(TraktContract.Episodes.SEASON));
                final int number = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));

                EpisodeWrapper.setIsInWatchlist(mContext.getContentResolver(), episodeId, inWatchlist);

                postPriorityTask(new EpisodeWatchlistTask(tvdbId, season, number, inWatchlist));
            }
        });
    }

    /**
     * Check into a show on trakt. Think of this method as in between a seen and a scrobble. After checking in,
     * the trakt will automatically display it as watching then switch over to watched status once the duration
     * has elapsed.
     *
     * @param episodeId The database id of the episode.
     */
    public void checkin(final long episodeId) {
        execute(new Runnable() {
            @Override
            public void run() {
                // TODO
            }
        });
    }

    /**
     * Notify trakt that user wants to cancel their current check in.
     *
     * @param episodeId The database id of the episode.
     */
    public void cancelCheckin(final long episodeId) {
        execute(new Runnable() {
            @Override
            public void run() {
                // TODO
            }
        });
    }

    /**
     * Notify trakt that user has started watching a show.
     *
     * @param episodeId The database id of the episode.
     */
    public void watching(final long episodeId) {
        // TODO
    }

    /**
     * Notify trakt that user has stopped watching a show.
     *
     * @param episodeId The database id of the episode.
     */
    public void cancelWatching(final long episodeId) {
        // TODO
    }

    /**
     * Notify trakt that a user has finished watching a show. This commits the show to the users profile.
     * You should use {@link #watching(long)} prior to calling this method.
     *
     * @param episodeId The database id of the episode.
     */
    public void scrobble(final long episodeId) {
        execute(new Runnable() {
            @Override
            public void run() {
                // TODO
            }
        });
    }

    /**
     * Rate an episode on trakt. Depending on the user settings, this will also send out social updates to facebook,
     * twitter, and tumblr.
     *
     * @param episodeId The database id of the episode.
     * @param rating    A rating betweeo 1 and 10. Use 0 to undo rating.
     */
    public void rate(final long episodeId, final int rating) {
        execute(new Runnable() {
            @Override
            public void run() {
                final long tvdbId = EpisodeWrapper.getShowTvdbId(mContext.getContentResolver(), episodeId);
                Cursor c = EpisodeWrapper.query(mContext.getContentResolver(), episodeId,
                        TraktContract.Episodes.EPISODE);

                if (c.moveToFirst()) {
                    final int episode = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));

                    ContentValues cv = new ContentValues();
                    cv.put(TraktContract.Episodes.RATING, rating);
                    mContext.getContentResolver().update(TraktContract.Episodes.buildFromId(episodeId), cv, null, null);

                    mQueue.add(new EpisodeRateTask(tvdbId, episode, rating));
                }
            }
        });
        // TODO:
    }
}
