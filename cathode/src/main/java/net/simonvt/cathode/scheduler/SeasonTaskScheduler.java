package net.simonvt.cathode.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.remote.action.EpisodeCollectionTask;
import net.simonvt.cathode.remote.action.EpisodeWatchedTask;

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
            .query(CathodeContract.Episodes.buildFromSeasonId(seasonId), new String[] {
                CathodeContract.Episodes._ID, CathodeContract.Episodes.WATCHED,
                CathodeContract.Episodes.SEASON, CathodeContract.Episodes.EPISODE,
            }, null, null, null);

        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Episodes.WATCHED, watched);
        context.getContentResolver()
            .update(CathodeContract.Episodes.buildFromSeasonId(seasonId), cv, null, null);

        while (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndex(CathodeContract.Episodes._ID));
          final boolean episodeWatched =
              c.getLong(c.getColumnIndex(CathodeContract.Episodes.WATCHED)) != 0;
          final int season = c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON));
          final int episode = c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE));
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
            .query(CathodeContract.Episodes.buildFromSeasonId(seasonId), new String[] {
                CathodeContract.Episodes._ID, CathodeContract.Episodes.IN_COLLECTION,
                CathodeContract.Episodes.SEASON, CathodeContract.Episodes.EPISODE,
            }, null, null, null);

        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Episodes.IN_COLLECTION, inCollection);
        context.getContentResolver()
            .update(CathodeContract.Episodes.buildFromSeasonId(seasonId), cv, null, null);

        while (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndex(CathodeContract.Episodes._ID));
          final boolean episodeInCollection =
              c.getLong(c.getColumnIndex(CathodeContract.Episodes.IN_COLLECTION)) != 0;
          final int season = c.getInt(c.getColumnIndex(CathodeContract.Episodes.SEASON));
          final int episode = c.getInt(c.getColumnIndex(CathodeContract.Episodes.EPISODE));
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
