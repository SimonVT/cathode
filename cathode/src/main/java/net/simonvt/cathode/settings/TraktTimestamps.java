/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.simonvt.cathode.api.entity.LastActivity;
import net.simonvt.cathode.util.DateUtils;

public final class TraktTimestamps {

  private TraktTimestamps() {
  }

  public static void updateLastActivityStreamSync(Context context, long lastSync) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    settings.edit().putLong(Settings.ACTIVITY_STREAM_SYNC, lastSync).apply();
  }

  public static long lastActivityStreamSync(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    return settings.getLong(Settings.ACTIVITY_STREAM_SYNC, -1);
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

  public static boolean episodeRatingsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.EPISODE_RATING, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean episodeCommentsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.EPISODE_COMMENT, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean seasonRatingsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.SEASON_RATING, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean seasonCommentsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.SEASON_COMMENT, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean showWatchlistNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.SHOW_WATCHLIST, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean showRatingsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.SEASON_RATING, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean showCommentsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.SHOW_COMMENT, -1);
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

  public static boolean movieRatingsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.MOVIE_RATING, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieCommentsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(Settings.MOVIE_COMMENT, -1);
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

  public static boolean shouldPurge(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    final long lastPurge = settings.getLong(Settings.LAST_PURGE, 0);
    if (lastPurge == 0) {
      return true;
    }

    final boolean shouldPurge =
        System.currentTimeMillis() > lastPurge + 7 * DateUtils.DAY_IN_MILLIS;

    if (shouldPurge) {
      settings.edit().putLong(Settings.LAST_PURGE, System.currentTimeMillis()).apply();
    }

    return shouldPurge;
  }

  public static void update(Context context, LastActivity lastActivity) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    editor.putLong(Settings.SHOW_RATING, lastActivity.getShows().getRatedAt().getTimeInMillis());
    editor.putLong(Settings.SHOW_WATCHLIST,
        lastActivity.getShows().getWatchlistedAt().getTimeInMillis());
    editor.putLong(Settings.SHOW_COMMENT, lastActivity.getShows().getCommentedAt().getTimeInMillis());

    editor.putLong(Settings.SEASON_COMMENT,
        lastActivity.getSeasons().getCommentedAt().getTimeInMillis());
    editor.putLong(Settings.SEASON_RATING,
        lastActivity.getSeasons().getRatedAt().getTimeInMillis());

    editor.putLong(Settings.EPISODE_WATCHED,
        lastActivity.getEpisodes().getWatchedAt().getTimeInMillis());
    editor.putLong(Settings.EPISODE_COLLECTION,
        lastActivity.getEpisodes().getCollectedAt().getTimeInMillis());
    editor.putLong(Settings.EPISODE_RATING,
        lastActivity.getEpisodes().getRatedAt().getTimeInMillis());
    editor.putLong(Settings.EPISODE_WATCHLIST,
        lastActivity.getEpisodes().getWatchlistedAt().getTimeInMillis());
    editor.putLong(Settings.EPISODE_COMMENT,
        lastActivity.getEpisodes().getCommentedAt().getTimeInMillis());

    editor.putLong(Settings.MOVIE_WATCHED, lastActivity.getMovies().getWatchedAt().getTimeInMillis());
    editor.putLong(Settings.MOVIE_COLLECTION,
        lastActivity.getMovies().getCollectedAt().getTimeInMillis());
    editor.putLong(Settings.MOVIE_RATING, lastActivity.getMovies().getRatedAt().getTimeInMillis());
    editor.putLong(Settings.MOVIE_WATCHLIST,
        lastActivity.getMovies().getWatchlistedAt().getTimeInMillis());
    editor.putLong(Settings.MOVIE_COMMENT, lastActivity.getMovies().getCommentedAt().getTimeInMillis());

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
        .putLong(Settings.RECOMMENDATIONS, currentTimeMillis)
        .apply();
  }

  public static void clear(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    editor.remove(Settings.EPISODE_WATCHED);
    editor.remove(Settings.EPISODE_COLLECTION);
    editor.remove(Settings.EPISODE_RATING);
    editor.remove(Settings.EPISODE_WATCHLIST);
    editor.remove(Settings.EPISODE_COMMENT);

    editor.remove(Settings.SHOW_RATING);
    editor.remove(Settings.SHOW_WATCHLIST);
    editor.remove(Settings.SHOW_COMMENT);

    editor.remove(Settings.SEASON_COMMENT);
    editor.remove(Settings.SEASON_RATING);

    editor.remove(Settings.MOVIE_WATCHED);
    editor.remove(Settings.MOVIE_COLLECTION);
    editor.remove(Settings.MOVIE_RATING);
    editor.remove(Settings.MOVIE_WATCHLIST);
    editor.remove(Settings.MOVIE_COMMENT);

    editor.commit();
  }
}
