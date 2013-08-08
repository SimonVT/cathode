package net.simonvt.trakt.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.trakt.provider.EpisodeWrapper;
import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.remote.action.EpisodeCollectionTask;
import net.simonvt.trakt.remote.action.EpisodeWatchedTask;

public class SeasonTaskScheduler extends BaseTaskScheduler {

  @Inject EpisodeTaskScheduler episodeScheduler;

  public SeasonTaskScheduler(Context context) {
    super(context);
  }

  public void setWatched(final long seasonId, final boolean watched) {
    execute(new Runnable() {
      @Override
      public void run() {
        Cursor c = context.getContentResolver()
            .query(TraktContract.Episodes.buildFromSeasonId(seasonId), new String[] {
                TraktContract.Episodes._ID, TraktContract.Episodes.WATCHED,
                TraktContract.Episodes.SEASON, TraktContract.Episodes.EPISODE,
            }, null, null, null);

        ContentValues cv = new ContentValues();
        cv.put(TraktContract.Episodes.WATCHED, watched);
        context.getContentResolver()
            .update(TraktContract.Episodes.buildFromSeasonId(seasonId), cv, null, null);

        while (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndex(TraktContract.Episodes._ID));
          final boolean episodeWatched =
              c.getLong(c.getColumnIndex(TraktContract.Episodes.WATCHED)) != 0;
          final int season = c.getInt(c.getColumnIndex(TraktContract.Episodes.SEASON));
          final int episode = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));
          final int tvdbId = EpisodeWrapper.getShowTvdbId(context.getContentResolver(), episodeId);

          if (watched != episodeWatched) {
            postPriorityTask(new EpisodeWatchedTask(tvdbId, season, episode, watched));
          }
        }

        c.close();
      }
    });
  }

  public void setInCollection(final long seasonId, final boolean inCollection) {
    execute(new Runnable() {
      @Override
      public void run() {
        Cursor c = context.getContentResolver()
            .query(TraktContract.Episodes.buildFromSeasonId(seasonId), new String[] {
                TraktContract.Episodes._ID, TraktContract.Episodes.IN_COLLECTION,
                TraktContract.Episodes.SEASON, TraktContract.Episodes.EPISODE,
            }, null, null, null);

        ContentValues cv = new ContentValues();
        cv.put(TraktContract.Episodes.IN_COLLECTION, inCollection);
        context.getContentResolver()
            .update(TraktContract.Episodes.buildFromSeasonId(seasonId), cv, null, null);

        while (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndex(TraktContract.Episodes._ID));
          final boolean episodeInCollection =
              c.getLong(c.getColumnIndex(TraktContract.Episodes.IN_COLLECTION)) != 0;
          final int season = c.getInt(c.getColumnIndex(TraktContract.Episodes.SEASON));
          final int episode = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));
          final int tvdbId = EpisodeWrapper.getShowTvdbId(context.getContentResolver(), episodeId);

          if (inCollection != episodeInCollection) {
            postPriorityTask(new EpisodeCollectionTask(tvdbId, season, episode, inCollection));
          }
        }

        c.close();
      }
    });
  }
}
