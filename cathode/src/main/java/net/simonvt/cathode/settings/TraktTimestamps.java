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
import net.simonvt.cathode.settings.Settings.ActivityTimestamp;
import net.simonvt.cathode.util.DateUtils;

public final class TraktTimestamps {

  private TraktTimestamps() {
  }

  public static boolean episodeWatchedNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.EPISODE_WATCHED, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean episodeCollectedNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.EPISODE_COLLECTION, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean episodeWatchlistNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.EPISODE_WATCHLIST, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean episodeRatingsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.EPISODE_RATING, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean episodeCommentsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.EPISODE_COMMENT, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean seasonRatingsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.SEASON_RATING, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean seasonCommentsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.SEASON_COMMENT, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean showWatchlistNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.SHOW_WATCHLIST, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean showRatingsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.SEASON_RATING, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean showCommentsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.SHOW_COMMENT, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieWatchedNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.MOVIE_WATCHED, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieCollectedNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.MOVIE_COLLECTION, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieWatchlistNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.MOVIE_WATCHLIST, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieRatingsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.MOVIE_RATING, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean movieCommentsNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.MOVIE_COMMENT, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean commentLikedNeedsUpdate(Context context, long lastLiked) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.COMMENT_LIKED_AT, -1);
    return lastActivity == -1 || lastLiked > lastActivity;
  }

  public static boolean listNeedsUpdate(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    long lastActivity = settings.getLong(ActivityTimestamp.LIST_UPDATED_AT, -1);
    return lastActivity == -1 || lastUpdated > lastActivity;
  }

  public static boolean suggestionsNeedsUpdate(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    final long lastActivity = settings.getLong(Settings.SUGGESTIONS, -1);
    return System.currentTimeMillis() > lastActivity + 3 * DateUtils.HOUR_IN_MILLIS;
  }

  public static boolean hiddenNeedsUpdate(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    final long lastActivity = settings.getLong(Settings.LAST_SYNC_HIDDEN, -1);
    return System.currentTimeMillis() > lastActivity + 3 * DateUtils.HOUR_IN_MILLIS;
  }

  public static boolean shouldSyncComments(long lastSync) {
    return System.currentTimeMillis() > lastSync + 3 * DateUtils.HOUR_IN_MILLIS;
  }

  public static boolean shouldSyncPerson(long lastSync) {
    return System.currentTimeMillis() > lastSync + 24 * DateUtils.HOUR_IN_MILLIS;
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

    editor.putLong(ActivityTimestamp.SHOW_RATING,
        lastActivity.getShows().getRatedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.SHOW_WATCHLIST,
        lastActivity.getShows().getWatchlistedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.SHOW_COMMENT,
        lastActivity.getShows().getCommentedAt().getTimeInMillis());

    editor.putLong(ActivityTimestamp.SEASON_COMMENT,
        lastActivity.getSeasons().getCommentedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.SEASON_RATING,
        lastActivity.getSeasons().getRatedAt().getTimeInMillis());

    editor.putLong(ActivityTimestamp.EPISODE_WATCHED,
        lastActivity.getEpisodes().getWatchedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.EPISODE_COLLECTION,
        lastActivity.getEpisodes().getCollectedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.EPISODE_RATING,
        lastActivity.getEpisodes().getRatedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.EPISODE_WATCHLIST,
        lastActivity.getEpisodes().getWatchlistedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.EPISODE_COMMENT,
        lastActivity.getEpisodes().getCommentedAt().getTimeInMillis());

    editor.putLong(ActivityTimestamp.MOVIE_WATCHED,
        lastActivity.getMovies().getWatchedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.MOVIE_COLLECTION,
        lastActivity.getMovies().getCollectedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.MOVIE_RATING,
        lastActivity.getMovies().getRatedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.MOVIE_WATCHLIST,
        lastActivity.getMovies().getWatchlistedAt().getTimeInMillis());
    editor.putLong(ActivityTimestamp.MOVIE_COMMENT,
        lastActivity.getMovies().getCommentedAt().getTimeInMillis());

    editor.putLong(ActivityTimestamp.COMMENT_LIKED_AT,
        lastActivity.getComments().getLikedAt().getTimeInMillis());

    editor.putLong(ActivityTimestamp.LIST_UPDATED_AT,
        lastActivity.getLists().getUpdatedAt().getTimeInMillis());

    editor.apply();
  }

  public static void updateSuggestions(Context context) {
    final long currentTimeMillis = System.currentTimeMillis();
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putLong(Settings.SUGGESTIONS, currentTimeMillis)
        .apply();
  }

  public static void updateHidden(Context context) {
    final long currentTimeMillis = System.currentTimeMillis();
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putLong(Settings.LAST_SYNC_HIDDEN, currentTimeMillis)
        .apply();
  }

  public static void clear(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    editor.remove(ActivityTimestamp.EPISODE_WATCHED);
    editor.remove(ActivityTimestamp.EPISODE_COLLECTION);
    editor.remove(ActivityTimestamp.EPISODE_RATING);
    editor.remove(ActivityTimestamp.EPISODE_WATCHLIST);
    editor.remove(ActivityTimestamp.EPISODE_COMMENT);

    editor.remove(ActivityTimestamp.SHOW_RATING);
    editor.remove(ActivityTimestamp.SHOW_WATCHLIST);
    editor.remove(ActivityTimestamp.SHOW_COMMENT);

    editor.remove(ActivityTimestamp.SEASON_COMMENT);
    editor.remove(ActivityTimestamp.SEASON_RATING);

    editor.remove(ActivityTimestamp.MOVIE_WATCHED);
    editor.remove(ActivityTimestamp.MOVIE_COLLECTION);
    editor.remove(ActivityTimestamp.MOVIE_RATING);
    editor.remove(ActivityTimestamp.MOVIE_WATCHLIST);
    editor.remove(ActivityTimestamp.MOVIE_COMMENT);

    editor.remove(ActivityTimestamp.COMMENT_LIKED_AT);

    editor.remove(ActivityTimestamp.LIST_UPDATED_AT);

    editor.apply();
  }
}
