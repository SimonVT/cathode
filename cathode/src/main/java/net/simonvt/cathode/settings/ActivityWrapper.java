package net.simonvt.cathode.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.simonvt.cathode.api.entity.LastActivity;
import net.simonvt.cathode.util.DateUtils;

public final class ActivityWrapper {

  private static final String TAG = "ActivityWrapper";

  private ActivityWrapper() {
  }

  public static boolean episodeWatchedNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.EPISODE_WATCHED, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean episodeCollectedNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.EPISODE_COLLECTION, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean episodeWatchlistNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.EPISODE_WATCHLIST, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean showWatchlistNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.SHOW_WATCHLIST, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieWatchedNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.MOVIE_WATCHED, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieCollectedNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.MOVIE_COLLECTION, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieWatchlistNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.MOVIE_WATCHLIST, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean trendingNeedsUpdate(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    final long lastActivity = settings.getLong(Settings.TRENDING, -1);
    return System.currentTimeMillis() > lastActivity + 3 * DateUtils.HOUR_IN_MILLIS;
  }

  public static boolean recommendationsNeedsUpdate(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    final long lastActivity = settings.getLong(Settings.RECOMMENDATIONS, -1);
    return System.currentTimeMillis() > lastActivity + 3 * DateUtils.HOUR_IN_MILLIS;
  }

  public static void update(Context context, LastActivity lastActivity) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    editor.putLong(Settings.ALL, lastActivity.getAll());
    editor.putLong(Settings.EPISODE_WATCHED, lastActivity.getEpisode().getWatched());
    editor.putLong(Settings.EPISODE_SCROBBLE, lastActivity.getEpisode().getScrobble());
    editor.putLong(Settings.EPISODE_SEEN, lastActivity.getEpisode().getScrobble());
    editor.putLong(Settings.EPISODE_CHECKIN, lastActivity.getEpisode().getCheckin());
    editor.putLong(Settings.EPISODE_COLLECTION, lastActivity.getEpisode().getCollection());
    editor.putLong(Settings.EPISODE_RATING, lastActivity.getEpisode().getRating());
    editor.putLong(Settings.EPISODE_WATCHLIST, lastActivity.getEpisode().getWatchlist());
    editor.putLong(Settings.EPISODE_COMMENT, lastActivity.getEpisode().getComment());
    editor.putLong(Settings.EPISODE_REVIEW, lastActivity.getEpisode().getReview());
    editor.putLong(Settings.EPISODE_SHOUT, lastActivity.getEpisode().getShout());

    editor.putLong(Settings.SHOW_RATING, lastActivity.getShow().getRating());
    editor.putLong(Settings.SHOW_WATCHLIST, lastActivity.getShow().getWatchlist());
    editor.putLong(Settings.SHOW_COMMENT, lastActivity.getShow().getComment());
    editor.putLong(Settings.SHOW_REVIEW, lastActivity.getShow().getReview());
    editor.putLong(Settings.SHOW_SHOUT, lastActivity.getShow().getShout());

    editor.putLong(Settings.MOVIE_WATCHED, lastActivity.getMovie().getWatched());
    editor.putLong(Settings.MOVIE_SCROBBLE, lastActivity.getMovie().getScrobble());
    editor.putLong(Settings.MOVIE_SEEN, lastActivity.getMovie().getSeen());
    editor.putLong(Settings.MOVIE_CHECKIN, lastActivity.getMovie().getCheckin());
    editor.putLong(Settings.MOVIE_COLLECTION, lastActivity.getMovie().getCollection());
    editor.putLong(Settings.MOVIE_RATING, lastActivity.getMovie().getRating());
    editor.putLong(Settings.MOVIE_WATCHLIST, lastActivity.getMovie().getWatchlist());
    editor.putLong(Settings.MOVIE_COMMENT, lastActivity.getMovie().getComment());
    editor.putLong(Settings.MOVIE_REVIEW, lastActivity.getMovie().getReview());
    editor.putLong(Settings.MOVIE_SHOUT, lastActivity.getMovie().getShout());

    editor.commit();
  }

  public static void updateTrending(Context context) {
    final long currentTimeMillis = System.currentTimeMillis();
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putLong(Settings.TRENDING, currentTimeMillis)
        .apply();
  }

  public static void updateRecommendations(Context context) {
    final long currentTimeMillis = System.currentTimeMillis();
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putLong(Settings.TRENDING, currentTimeMillis)
        .apply();
  }

  public static void clear(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    editor.remove(Settings.ALL);
    editor.remove(Settings.EPISODE_WATCHED);
    editor.remove(Settings.EPISODE_SCROBBLE);
    editor.remove(Settings.EPISODE_SEEN);
    editor.remove(Settings.EPISODE_CHECKIN);
    editor.remove(Settings.EPISODE_COLLECTION);
    editor.remove(Settings.EPISODE_RATING);
    editor.remove(Settings.EPISODE_WATCHLIST);
    editor.remove(Settings.EPISODE_COMMENT);
    editor.remove(Settings.EPISODE_REVIEW);
    editor.remove(Settings.EPISODE_SHOUT);

    editor.remove(Settings.SHOW_RATING);
    editor.remove(Settings.SHOW_WATCHLIST);
    editor.remove(Settings.SHOW_COMMENT);
    editor.remove(Settings.SHOW_REVIEW);
    editor.remove(Settings.SHOW_SHOUT);

    editor.remove(Settings.MOVIE_WATCHED);
    editor.remove(Settings.MOVIE_SCROBBLE);
    editor.remove(Settings.MOVIE_SEEN);
    editor.remove(Settings.MOVIE_CHECKIN);
    editor.remove(Settings.MOVIE_COLLECTION);
    editor.remove(Settings.MOVIE_RATING);
    editor.remove(Settings.MOVIE_WATCHLIST);
    editor.remove(Settings.MOVIE_COMMENT);
    editor.remove(Settings.MOVIE_REVIEW);
    editor.remove(Settings.MOVIE_SHOUT);

    editor.commit();
  }
}
