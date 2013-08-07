package net.simonvt.trakt.scheduler;

import android.content.ContentValues;
import android.content.Context;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.remote.action.MovieCollectionTask;
import net.simonvt.trakt.remote.action.MovieRateTask;
import net.simonvt.trakt.remote.action.MovieWatchedTask;
import net.simonvt.trakt.remote.action.MovieWatchlistTask;

public class MovieTaskScheduler extends BaseTaskScheduler {

  public MovieTaskScheduler(Context context) {
    super(context);
  }

  /**
   * Add episodes watched outside of trakt to user library.
   *
   * @param movieId The database id of the episode.
   * @param watched Whether the episode has been watched.
   */
  public void setWatched(final long movieId, final boolean watched) {
    execute(new Runnable() {
      @Override
      public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);

        MovieWrapper.setWatched(context.getContentResolver(), movieId, watched);
        if (watched) MovieWrapper.setIsInWatchlist(context.getContentResolver(), movieId, false);

        postPriorityTask(new MovieWatchedTask(tmdbId, watched));
      }
    });
  }

  public void setIsInWatchlist(final long movieId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override
      public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);

        MovieWrapper.setIsInWatchlist(context.getContentResolver(), movieId, inWatchlist);

        postPriorityTask(new MovieWatchlistTask(tmdbId, inWatchlist));
      }
    });
  }

  public void setIsInCollection(final long movieId, final boolean inCollection) {
    execute(new Runnable() {
      @Override
      public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);

        MovieWrapper.setIsInCollection(context.getContentResolver(), movieId, inCollection);

        postPriorityTask(new MovieCollectionTask(tmdbId, inCollection));
      }
    });
  }

  /**
   * Check into a movie on trakt. Think of this method as in between a seen and a scrobble.
   * After checking in, the trakt will automatically display it as watching then switch over to
   * watched status once
   * the duration has elapsed.
   *
   * @param context The Context this method is called from.
   * @param movieId The database id of the movie.
   */
  public static void checkin(Context context, long movieId) {
    // TODO:
  }

  /**
   * Notify trakt that user wants to cancel their current check in.
   *
   * @param context The Context this method is called from.
   * @param movieId The database id of the movie.
   */
  public static void cancelCheckin(Context context, long movieId) {
    // TODO:
  }

  /**
   * Notify trakt that user has started watching a movie.
   *
   * @param context The Context this method is called from.
   * @param movieId The database id of the movie.
   */
  public static void watching(Context context, long movieId) {
    // TODO:
  }

  /**
   * Notify trakt that user has stopped watching a movie.
   *
   * @param context The Context this method is called from.
   * @param movieId The database id of the movie.
   */
  public static void cancelWatching(Context context, long movieId) {
    // TODO:
  }

  /**
   * Notify trakt that a user has finished watching a movie. This commits the movie to the users
   * profile.
   * Use {@link #watching(android.content.Context, long)} prior to calling this method.
   *
   * @param context The Context this method is called from.
   * @param movieId The database id of the movie.
   */
  public static void scrobble(Context context, long movieId) {
    // TODO:
  }

  /**
   * Rate a movie on trakt. Depending on the user settings, this will also send out social updates
   * to facebook,
   * twitter, and tumblr.
   *
   * @param movieId The database id of the movie.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  public void rate(final long movieId, final int rating) {
    execute(new Runnable() {
      @Override
      public void run() {
        final long tmdbId = MovieWrapper.getTmdbId(context.getContentResolver(), movieId);

        ContentValues cv = new ContentValues();
        cv.put(TraktContract.Movies.RATING, rating);
        context.getContentResolver()
            .update(TraktContract.Movies.buildMovieUri(movieId), cv, null, null);

        queue.add(new MovieRateTask(tmdbId, rating));
      }
    });
    // TODO:
  }
}
