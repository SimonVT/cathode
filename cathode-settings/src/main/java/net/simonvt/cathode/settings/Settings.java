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

public final class Settings {

  private Settings() {
  }

  public static final String START_PAGE = "startPage";

  public static final String UPCOMING_TIME = "upcomingTime";
  public static final String SHOWS_OFFSET = "showsOffset";

  public static final String SHOWS_AVOID_SPOILERS = "showsAvoidSpoilers";

  public static final String VERSION_CODE = "versionCode";

  // Whether upcoming shows should be added to the calendar
  public static final String CALENDAR_SYNC = "calendarSync";
  public static final String CALENDAR_COLOR = "calendarColor";
  public static final int CALENDAR_COLOR_DEFAULT = 0xff34495e;
  public static final int[] CALENDAR_COLORS = new int[] {
      0xff34495e, 0xffd50000, 0xfff4511e, 0xffef6c00, 0xfff09300, 0xfff6bf26, 0xffe4c441,
      0xffc0ca33, 0xff7cb342, 0xff0b8043, 0xff009688, 0xff33b679, 0xff039be5, 0xff4285f4,
      0xff3f51b5, 0xff7986cb, 0xffb39ddb, 0xff9e69af, 0xff8e24aa, 0xffad1457, 0xffd81b60,
      0xffe67c73, 0xff795548, 0xff616161, 0xffa79b8e,
  };
  public static final String CALENDAR_COLOR_NEEDS_UPDATE = "calendarColorNeedsUpdate";

  // Notifications
  public static final String NOTIFICACTIONS_ENABLED = "notificationsEnabled";
  public static final String NOTIFICACTION_TIME = "notificationTime";
  public static final String NOTIFICACTION_VIBRATE = "notificationVibrate";
  public static final String NOTIFICACTION_SOUND = "notificationSound";

  public static SharedPreferences get(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public interface Sort {

    String SHOW_UPCOMING = "sortShowUpcoming";
    String SHOW_TRENDING = "sortShowTrending";
    String SHOW_RECOMMENDED = "sortShowRecommended";
    String SHOW_ANTICIPATED = "sortShowAnticipated";
    String SHOW_WATCHED = "sortShowWatched";
    String SHOW_COLLECTED = "sortShowCollected";

    String MOVIE_SEARCH = "sortMovieSearch";
    String MOVIE_TRENDING = "sortMovieTrending";
    String MOVIE_RECOMMENDED = "sortMovieRecommended";
    String MOVIE_ANTICIPATED = "sortMovieAnticipated";
    String MOVIE_WATCHED = "sortMovieCollected";
    String MOVIE_COLLECTED = "sortMovieCollected";

    String SEARCH = "sortSearch";
  }

  public static void clearSettings(Context context) {
    SharedPreferences.Editor editor = get(context).edit();

    editor.remove(TraktLinkSettings.TRAKT_LINKED);
    editor.remove(TraktLinkSettings.TRAKT_AUTH_FAILED);
    editor.remove(TraktLinkSettings.TRAKT_ACCESS_TOKEN);
    editor.remove(TraktLinkSettings.TRAKT_REFRESH_TOKEN);
    editor.remove(TraktLinkSettings.TRAKT_TOKEN_EXPIRATION);

    editor.remove(START_PAGE);

    editor.remove(CALENDAR_SYNC);
    editor.remove(CALENDAR_COLOR);
    editor.remove(CALENDAR_COLOR_NEEDS_UPDATE);

    // Clear sorting options
    editor.remove(Sort.SHOW_UPCOMING);
    editor.remove(Sort.SEARCH);
    editor.remove(Sort.SHOW_TRENDING);
    editor.remove(Sort.SHOW_RECOMMENDED);
    editor.remove(Sort.SHOW_WATCHED);
    editor.remove(Sort.SHOW_COLLECTED);

    editor.remove(Sort.MOVIE_SEARCH);
    editor.remove(Sort.MOVIE_TRENDING);
    editor.remove(Sort.MOVIE_RECOMMENDED);
    editor.remove(Sort.MOVIE_WATCHED);
    editor.remove(Sort.MOVIE_COLLECTED);

    editor.apply();
  }
}
