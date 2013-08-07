package net.simonvt.trakt.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.SeasonWrapper;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.remote.action.EpisodeCollectionTask;
import net.simonvt.trakt.remote.action.EpisodeWatchedTask;
import net.simonvt.trakt.remote.action.ShowCollectionTask;
import net.simonvt.trakt.remote.action.ShowRateTask;
import net.simonvt.trakt.remote.action.ShowWatchedTask;
import net.simonvt.trakt.remote.action.ShowWatchlistTask;
import net.simonvt.trakt.remote.sync.SyncShowTask;

public class ShowTaskScheduler extends BaseTaskScheduler {

  @Inject EpisodeTaskScheduler episodeScheduler;

  public ShowTaskScheduler(Context context) {
    super(context);
    TraktApp.inject(context, this);
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
            .query(TraktContract.Episodes.buildFromShowId(showId), new String[] {
                TraktContract.Episodes._ID, TraktContract.Episodes.SEASON,
                TraktContract.Episodes.EPISODE,
            }, "watched=0 AND season<>0", null, TraktContract.Episodes.SEASON
                + " ASC, "
                + TraktContract.Episodes.EPISODE
                + " ASC LIMIT 1");

        if (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndexOrThrow(TraktContract.Episodes._ID));
          episodeScheduler.setWatched(episodeId, true);

          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
          final int season = c.getInt(c.getColumnIndexOrThrow(TraktContract.Episodes.SEASON));
          final int number = c.getInt(c.getColumnIndexOrThrow(TraktContract.Episodes.EPISODE));
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
            .query(TraktContract.Episodes.buildFromShowId(showId), new String[] {
                TraktContract.Episodes._ID, TraktContract.Episodes.SEASON,
                TraktContract.Episodes.EPISODE,
            }, "inCollection=0 AND season<>0", null, TraktContract.Episodes.SEASON
                + " ASC, "
                + TraktContract.Episodes.EPISODE
                + " ASC LIMIT 1");

        if (c.moveToNext()) {
          final long episodeId = c.getLong(c.getColumnIndexOrThrow(TraktContract.Episodes._ID));
          episodeScheduler.setIsInCollection(episodeId, true);

          final int tvdbId = ShowWrapper.getTvdbId(context.getContentResolver(), showId);
          final int season = c.getInt(c.getColumnIndexOrThrow(TraktContract.Episodes.SEASON));
          final int number = c.getInt(c.getColumnIndexOrThrow(TraktContract.Episodes.EPISODE));
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
            .query(TraktContract.Shows.buildShowUri(showId), new String[] {
                TraktContract.Shows.TVDB_ID,
            }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(TraktContract.Shows.TVDB_ID));
          ShowWrapper.setWatched(context.getContentResolver(), showId, watched);
          queue.add(new ShowWatchedTask(tvdbId, watched));

          Cursor seasons = context.getContentResolver()
              .query(TraktContract.Seasons.buildFromShowId(showId), new String[] {
                  TraktContract.Seasons._ID,
              }, null, null, null);

          while (seasons.moveToNext()) {
            SeasonWrapper.updateSeasonCounts(context.getContentResolver(), seasons.getLong(0));
          }
          seasons.close();

          ShowWrapper.updateShowCounts(context.getContentResolver(), showId);
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
            .query(TraktContract.Shows.buildShowUri(showId), new String[] {
                TraktContract.Shows.TVDB_ID,
            }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(TraktContract.Shows.TVDB_ID));
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
            .query(TraktContract.Shows.buildShowUri(showId), new String[] {
                TraktContract.Shows.TVDB_ID,
            }, null, null, null);

        if (c.moveToFirst()) {
          final int tvdbId = c.getInt(c.getColumnIndex(TraktContract.Shows.TVDB_ID));
          ShowWrapper.setIsInCollection(context.getContentResolver(), showId, inCollection);
          queue.add(new ShowCollectionTask(tvdbId, inCollection));

          Cursor seasons = context.getContentResolver()
              .query(TraktContract.Seasons.buildFromShowId(showId), new String[] {
                  TraktContract.Seasons._ID,
              }, null, null, null);

          while (seasons.moveToNext()) {
            SeasonWrapper.updateSeasonCounts(context.getContentResolver(), seasons.getLong(0));
          }
          seasons.close();

          ShowWrapper.updateShowCounts(context.getContentResolver(), showId);
        }

        c.close();
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
        cv.put(TraktContract.Shows.RATING, rating);
        context.getContentResolver()
            .update(TraktContract.Shows.buildShowUri(showId), cv, null, null);

        queue.add(new ShowRateTask(tvdbId, rating));
      }
    });
    // TODO:
  }
}
