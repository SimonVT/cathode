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
import com.uwetrottmann.tmdb2.entities.Configuration;
import java.util.Set;
import net.simonvt.cathode.api.entity.Account;
import net.simonvt.cathode.api.entity.Connections;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.SharingText;
import net.simonvt.cathode.api.entity.UserSettings;
import net.simonvt.cathode.images.ImageSizeSelector;
import net.simonvt.cathode.util.Lists;

public final class Settings {

  private Settings() {
  }

  public static final String TRAKT_LOGGED_IN = "traktLoggedIn";
  public static final String TRAKT_ACCESS_TOKEN = "traktToken";
  public static final String TRAKT_REFRESH_TOKEN = "traktRefreshToken";
  public static final String TRAKT_TOKEN_EXPIRATION = "traktTokenExpiration";

  public static final String START_PAGE = "startPage";

  public static final String UPCOMING_TIME = "upcomingTime";
  public static final String SHOWS_OFFSET = "showsOffset";

  public static final String SHOWS_AVOID_SPOILERS = "showsAvoidSpoilers";

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

  // Whether hidden shows should be displayed in upcoming
  public static final String SHOW_HIDDEN = "showHidden";

  // Last trending and recommendations sync
  public static final String SUGGESTIONS = "suggestions";
  public static final String LAST_SYNC_HIDDEN = "lastSyncHidden";

  // Settings not related to the user
  public static final String LAST_FULL_SYNC = "fullSync";
  public static final String LAST_PURGE = "lastPurge";
  public static final String VERSION_CODE = "versionCode";
  public static final String SHOWS_LAST_UPDATED = "showsLastUpdated";
  public static final String MOVIES_LAST_UPDATED = "showsLastUpdated";

  // tmdb config
  public static final String TMDB_LAST_CONFIGURATION_UPDATE = "tmdbLastConfigurationUpdate";

  public static final String TMDB_IMAGES_BASE_URL = "tmdbImagesBaseUrl";
  public static final String TMDB_IMAGES_SECURE_BASE_URL = "tmdbImagesSecureBaseUrl";
  public static final String TMDB_IMAGES_BACKDROP_SIZES = "tmdbImagesBackdropSizes";
  public static final String TMDB_IMAGES_LOGO_SIZES = "tmdbImagesLogoSizes";
  public static final String TMDB_IMAGES_STILL_SIZES = "tmdbImagesStillSizes";
  public static final String TMDB_IMAGES_POSTER_SIZES = "tmdbImagesPosterSizes";
  public static final String TMDB_IMAGES_PROFILE_SIZES = "tmdbImagesProfileSizes";

  public interface ActivityTimestamp {

    String SHOW_RATING = "showRating";
    String SHOW_WATCHLIST = "showWatchlist";
    String SHOW_COMMENT = "showComment";

    String SEASON_RATING = "seasonRating";
    String SEASON_COMMENT = "seasonComment";

    String EPISODE_WATCHED = "episodeWatched";
    String EPISODE_COLLECTION = "episodeCollection";
    String EPISODE_RATING = "episodeRating";
    String EPISODE_WATCHLIST = "episodeWatchlist";
    String EPISODE_COMMENT = "episodeComment";

    String MOVIE_WATCHED = "movieWatched";
    String MOVIE_COLLECTION = "movieCollection";
    String MOVIE_RATING = "movieRating";
    String MOVIE_WATCHLIST = "movieWatchlist";
    String MOVIE_COMMENT = "movieComment";

    String COMMENT_LIKED_AT = "commentLikedAt";

    String LIST_UPDATED_AT = "listUpdatedAt";
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

  public interface Profile {

    // User settings returned by account/settings
    String USERNAME = "profileUsername";
    String FULL_NAME = "profileFullName";
    String GENDER = "profileGender";
    String AGE = "profileAge";
    String LOCATION = "profileLocation";
    String ABOUT = "profileAbout";
    String JOINED = "profileJoined";
    String AVATAR = "profileAvatar";
    String PRIVATE = "profilePrivate";
    String VIP = "profileVip";
    String TIMEZONE = "profileTimezone";
    String COVER_IMAGE = "profileCoverImage";

    String CONNECTION_FACEBOOK = "profileConnectionFacebook";
    String CONNECTION_TWITTER = "profileConnectionTwitter";
    String CONNECTION_TUMBLR = "profileConnectionTumblr";

    String SHARING_TEXT_WATCHING = "profileSharingTextWatching";
    String SHARING_TEXT_WATCHED = "profileSharingTextWatched";
  }

  public static boolean isLoggedIn(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    return settings.getBoolean(TRAKT_LOGGED_IN, false);
  }

  public static void updateProfile(Context context, UserSettings userSettings) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    net.simonvt.cathode.api.entity.Profile profile = userSettings.getUser();
    if (profile != null) {
      putString(editor, Profile.USERNAME, profile.getUsername());
      putString(editor, Profile.FULL_NAME, profile.getName());
      putString(editor, Profile.GENDER, profile.getGender());
      putInt(editor, Profile.AGE, profile.getAge());
      putString(editor, Profile.LOCATION, profile.getLocation());
      putString(editor, Profile.ABOUT, profile.getAbout());
      putLong(editor, Profile.JOINED, profile.getJoinedAt().getTimeInMillis());

      putBoolean(editor, Profile.PRIVATE, profile.isPrivate());
      putBoolean(editor, Profile.VIP, profile.isVip());

      Images images = profile.getImages();
      putString(editor, Profile.AVATAR, images.getAvatar().getFull());
    }

    Account account = userSettings.getAccount();
    if (account != null) {
      putString(editor, Profile.TIMEZONE, account.getTimezone());
      putString(editor, Profile.COVER_IMAGE, account.getCoverImage());
    }

    Connections connection = userSettings.getConnections();
    if (connection != null) {
      putBoolean(editor, Profile.CONNECTION_FACEBOOK, connection.getFacebook());
      putBoolean(editor, Profile.CONNECTION_TWITTER, connection.getTwitter());
      putBoolean(editor, Profile.CONNECTION_TUMBLR, connection.getTumblr());
    }

    SharingText sharingText = userSettings.getSharingText();
    if (sharingText != null) {
      putString(editor, Profile.SHARING_TEXT_WATCHING, sharingText.getWatching());
      putString(editor, Profile.SHARING_TEXT_WATCHED, sharingText.getWatched());
    }

    editor.apply();
  }

  public static void updateTmdbConfiguration(Context context, Configuration configuration) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    Configuration.ImagesConfiguration images = configuration.images;

    editor.putString(Settings.TMDB_IMAGES_BASE_URL, images.base_url);
    editor.putString(Settings.TMDB_IMAGES_SECURE_BASE_URL, images.secure_base_url);

    Set<String> backdropSizes = Lists.asSet(images.backdrop_sizes);
    Set<String> logoSizes = Lists.asSet(images.logo_sizes);
    Set<String> stillSizes = Lists.asSet(images.still_sizes);
    Set<String> posterSizes = Lists.asSet(images.poster_sizes);
    Set<String> profileSizes = Lists.asSet(images.profile_sizes);

    editor.putStringSet(Settings.TMDB_IMAGES_BACKDROP_SIZES, backdropSizes);
    editor.putStringSet(Settings.TMDB_IMAGES_LOGO_SIZES, logoSizes);
    editor.putStringSet(Settings.TMDB_IMAGES_STILL_SIZES, stillSizes);
    editor.putStringSet(Settings.TMDB_IMAGES_POSTER_SIZES, posterSizes);
    editor.putStringSet(Settings.TMDB_IMAGES_PROFILE_SIZES, profileSizes);

    editor.apply();

    ImageSizeSelector.getInstance(context).updateSizes();
  }

  private static void putString(SharedPreferences.Editor editor, String key, Object value) {
    if (value != null) {
      editor.putString(key, value.toString());
    } else {
      editor.remove(key);
    }
  }

  private static void putInt(SharedPreferences.Editor editor, String key, Integer value) {
    if (value != null) {
      editor.putInt(key, value);
    } else {
      editor.remove(key);
    }
  }

  private static void putLong(SharedPreferences.Editor editor, String key, Long value) {
    if (value != null) {
      editor.putLong(key, value);
    } else {
      editor.remove(key);
    }
  }

  private static void putBoolean(SharedPreferences.Editor editor, String key, Boolean value) {
    if (value != null) {
      editor.putBoolean(key, value);
    } else {
      editor.remove(key);
    }
  }

  public static void clearProfile(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    editor.remove(Profile.USERNAME);
    editor.remove(Profile.FULL_NAME);
    editor.remove(Profile.GENDER);
    editor.remove(Profile.AGE);
    editor.remove(Profile.LOCATION);
    editor.remove(Profile.ABOUT);
    editor.remove(Profile.JOINED);
    editor.remove(Profile.AVATAR);
    editor.remove(Profile.PRIVATE);
    editor.remove(Profile.VIP);
    editor.remove(Profile.TIMEZONE);
    editor.remove(Profile.COVER_IMAGE);
    editor.remove(Profile.CONNECTION_FACEBOOK);
    editor.remove(Profile.CONNECTION_TWITTER);
    editor.remove(Profile.CONNECTION_TUMBLR);
    editor.remove(Profile.SHARING_TEXT_WATCHING);
    editor.remove(Profile.SHARING_TEXT_WATCHED);

    editor.apply();
  }

  public static void clearUserSettings(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    editor.remove(TRAKT_LOGGED_IN);
    editor.remove(TRAKT_ACCESS_TOKEN);
    editor.remove(TRAKT_REFRESH_TOKEN);

    editor.remove(START_PAGE);

    editor.remove(CALENDAR_SYNC);
    editor.remove(SHOW_HIDDEN);

    editor.remove(SUGGESTIONS);

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
