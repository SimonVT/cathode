package net.simonvt.trakt.scheduler;

import android.content.Context;

public class MovieTaskScheduler extends BaseTaskScheduler {

    public MovieTaskScheduler(Context context) {
        super(context);
    }

    /**
     * Sync data for movie with Trakt.
     *
     * @param context The Context the method is called from.
     * @param movieId The database id of the movie.
     */
    public static void sync(Context context, long movieId) {
        // TODO:
    }

    /**
     * Add movies watched outside of trakt to user library.
     *
     * @param context The Context this method is called from.
     * @param movieId The database id of the movie.
     */
    public static void seen(Context context, long movieId) {
        // TODO:
    }

    /**
     * Remove movies watched outside of trakt from user library.
     *
     * @param context The Context this method is called from.
     * @param movieId The database id of the movie.
     */
    public static void unseen(Context context, long movieId) {
        // TODO:
    }

    /**
     * Add movie to user library collection.
     *
     * @param context The Context this method is called from.
     * @param movieId The database id of the movie.
     */
    public static void library(Context context, long movieId) {
        // TODO:
    }

    /**
     * Remove movie from user library collection.
     *
     * @param context The Context this method is called from.
     * @param movieId The database id of the movie.
     */
    public static void unlibrary(Context context, long movieId) {
        // TODO:
    }

    /**
     * Add movie to user watchlist.
     *
     * @param context The Context this method is called from.
     * @param movieId The database id of the movie.
     */
    public static void watchlist(Context context, long movieId) {
        // TODO:
    }

    /**
     * Remove movie from user watchlist.
     *
     * @param context The Context this method is called from.
     * @param movieId The database id of the movie.
     */
    public static void unwatchlist(Context context, long movieId) {
        // TODO:
    }

    /**
     * Check into a movie on trakt. Think of this method as in between a seen and a scrobble.
     * After checking in, the trakt will automatically display it as watching then switch over to watched status once
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
     * Notify trakt that a user has finished watching a movie. This commits the movie to the users profile.
     * Use {@link #watching(android.content.Context, long)} prior to calling this method.
     *
     * @param context The Context this method is called from.
     * @param movieId The database id of the movie.
     */
    public static void scrobble(Context context, long movieId) {
        // TODO:
    }

    /**
     * Rate a movie on trakt. Depending on the user settings, this will also send out social updates to facebook,
     * twitter, and tumblr.
     *
     * @param context The Context the method is called from.
     * @param movieId The database id of the movie.
     * @param rating  A rating betweeo 1 and 10. Use 0 to undo rating.
     */
    public static void rate(Context context, long movieId, int rating) {
        // TODO:
    }

    /**
     * Rate a movie on trakt. Depending on the user settings, this will also send out social updates to facebook,
     * twitter, and tumblr.
     *
     * @param context The Context the method is called from.
     * @param movieId The database id of the movie.
     * @param rating  A value from {@link Rating}.
     */
    //    public static void rate(Context context, long movieId, Rating rating) {
    //        // TODO:
    //    }
}
