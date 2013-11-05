package net.simonvt.cathode.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.action.DismissShowRecommendation;
import net.simonvt.cathode.remote.action.EpisodeCollectionTask;
import net.simonvt.cathode.remote.action.EpisodeWatchedTask;
import net.simonvt.cathode.remote.action.ShowCollectionTask;
import net.simonvt.cathode.remote.action.ShowRateTask;
import net.simonvt.cathode.remote.action.ShowWatchedTask;
import net.simonvt.cathode.remote.action.ShowWatchlistTask;
import net.simonvt.cathode.remote.sync.SyncShowTask;

public class ShowTaskScheduler extends BaseTaskScheduler {

  @Inject EpisodeTaskScheduler episodeScheduler;

  public ShowTaskScheduler(Context context) {
    super(context);
    CathodeApp.inject(context, this);
  }

  /**
   * Sync data for show with Trakt.
   *
   * @param showId The database id of the show.
   */
  public void sync(final long showId) {
    execute(new Runnable() {
      @Override
      public void run() {
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
        postTask(new SyncShowTask(tvdbId));
      }
    });
  }

  public void watchedNext(final long showId) {
    execute(new Runnable() {
      @Override
      public void run() {
        Cursor c = context.getContentResolver()
            .query(CathodeContract.Episodes.buildFromShowId(showId), new String[] {
                CathodeContract.Episodes._ID, CathodeContract.Episodes.SEASON,
                CathodeContract.Episodes.EPISODE,
            }, "watched=0 AND season<>0", null, CathodeContract.Episodes.SEASON
                + " ASC, "
                + CathodeContract.Episodes.EPISODE
                + " ASC LIMIT 1");

        if (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndexOrThrow(CathodeContract.Episodes._ID));
          episodeScheduler.setWatched(episodeId, true);

          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
          final int season = c.getInt(c.getColumnIndexOrThrow(CathodeContract.Episodes.SEASON));
          final int number = c.getInt(c.getColumnIndexOrThrow(CathodeContract.Episodes.EPISODE));
          postPriorityTask(new EpisodeWatchedTask(tvdbId, season, number, true));
        }

        c.close();
      }
    });
  }

  public void collectedNext(final long showId) {
    execute(new Runnable() {
      @Override
      public void run() {
        Cursor c = context.getContentResolver()
            .query(CathodeContract.Episodes.buildFromShowId(showId), new String[] {
                CathodeContract.Episodes._ID, CathodeContract.Episodes.SEASON,
                CathodeContract.Episodes.EPISODE,
            }, "inCollection=0 AND season<>0", null, CathodeContract.Episodes.SEASON
                + " ASC, "
                + CathodeContract.Episodes.EPISODE
                + " ASC LIMIT 1");

        if (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndexOrThrow(CathodeContract.Episodes._ID));
          episodeScheduler.setIsInCollection(episodeId, true);

          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
          final int season = c.getInt(c.getColumnIndexOrThrow(CathodeContract.Episodes.SEASON));
          final int number = c.getInt(c.getColumnIndexOrThrow(CathodeContract.Episodes.EPISODE));
          postPriorityTask(new EpisodeCollectionTask(tvdbId, season, number, true));
        }

        c.close();
      }
    });
  }

  public void setWatched(final long showId, final boolean watched) {
    execute(new Runnable() {
      @Override
      public void run() {
        Cursor c = context.getContentResolver()
            .query(CathodeContract.Shows.buildFromId(showId), new String[] {
                CathodeContract.Shows.TVDB_ID,
            }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(CathodeContract.Shows.TVDB_ID));
          ShowWrapper.setWatched(context.getContentResolver(), showId, watched);
          queue.add(new ShowWatchedTask(tvdbId, watched));
        }

        c.close();
      }
    });
  }

  public void setIsInWatchlist(final long showId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override
      public void run() {
        Cursor c = context.getContentResolver()
            .query(CathodeContract.Shows.buildFromId(showId), new String[] {
                CathodeContract.Shows.TVDB_ID,
            }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(CathodeContract.Shows.TVDB_ID));
          ShowWrapper.setIsInWatchlist(context.getContentResolver(), showId, inWatchlist);
          queue.add(new ShowWatchlistTask(tvdbId, inWatchlist));
        }

        c.close();
      }
    });
  }

  public void setIsInCollection(final long showId, final boolean inCollection) {
    execute(new Runnable() {
      @Override
      public void run() {
        Cursor c = context.getContentResolver()
            .query(CathodeContract.Shows.buildFromId(showId), new String[] {
                CathodeContract.Shows.TVDB_ID,
            }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(CathodeContract.Shows.TVDB_ID));
          ShowWrapper.setIsInCollection(context.getContentResolver(), showId, inCollection);
          queue.add(new ShowCollectionTask(tvdbId, inCollection));
        }

        c.close();
      }
    });
  }

  public void dismissRecommendation(final long showId) {
    execute(new Runnable() {
      @Override public void run() {
        final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);

        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Shows.RECOMMENDATION_INDEX, -1);
        context.getContentResolver()
            .update(CathodeContract.Shows.buildFromId(showId), cv, null, null);

        queue.add(new DismissShowRecommendation(tvdbId));
      }
    });
  }

  /**
   * Rate a show on trakt. Depending on the user settings, this will also send out social updates
   * to facebook,
   * twitter, and tumblr.
   *
   * @param showId The database id of the show.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  public void rate(final long showId, final int rating) {
    execute(new Runnable() {
      @Override
      public void run() {
        final long tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);

        ContentValues cv = new ContentValues();
        cv.put(CathodeContract.Shows.RATING, rating);
        context.getContentResolver()
            .update(CathodeContract.Shows.buildFromId(showId), cv, null, null);

        queue.add(new ShowRateTask(tvdbId, rating));
      }
    });
    // TODO:
  }
}
