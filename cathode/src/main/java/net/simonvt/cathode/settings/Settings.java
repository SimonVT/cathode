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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import net.simonvt.cathode.api.entity.Account;
import net.simonvt.cathode.api.entity.Connections;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Profile;
import net.simonvt.cathode.api.entity.SharingText;
import net.simonvt.cathode.api.entity.UserSettings;

public class Settings extends PreferenceActivity {

  public static final String TRAKT_LOGGED_IN = "traktLoggedIn";
  public static final String TRAKT_TOKEN = "traktToken";

  public static final String SHOW_HIDDEN = "showHidden";

  public static final String SHOWS_LAST_UPDATED = "showsLastUpdated";

  public static final String MOVIES_LAST_UPDATED = "showsLastUpdated";

  public static final String VERSION_CODE = "versionCode";

  public static final String ACTIVITY_STREAM_SYNC = "activityStreamSync";
  public static final String FULL_SYNC = "fullSync";

  public static final String LAST_PURGE = "lastPurge";

  // For saving timestamps returned by sync/lastactivity
  public static final String SHOW_RATING = "showRating";
  public static final String SHOW_WATCHLIST = "showWatchlist";
  public static final String SHOW_COMMENT = "showComment";

  public static final String SEASON_RATING = "seasonRating";
  public static final String SEASON_COMMENT = "seasonComment";

  public static final String EPISODE_WATCHED = "episodeWatched";
  public static final String EPISODE_COLLECTION = "episodeCollection";
  public static final String EPISODE_RATING = "episodeRating";
  public static final String EPISODE_WATCHLIST = "episodeWatchlist";
  public static final String EPISODE_COMMENT = "episodeComment";

  public static final String MOVIE_WATCHED = "movieWatched";
  public static final String MOVIE_COLLECTION = "movieCollection";
  public static final String MOVIE_RATING = "movieRating";
  public static final String MOVIE_WATCHLIST = "movieWatchlist";
  public static final String MOVIE_COMMENT = "movieComment";

  // Last trending and recommendations sync
  public static final String TRENDING = "trending";
  public static final String RECOMMENDATIONS = "recommendations";

  // Whether the initial sync has been performed
  public static final String INITIAL_SYNC = "initial Sync";

  // Sorting options
  public static final String SORT_SHOW_UPCOMING = "sortShowUpcoming";
  public static final String SORT_SHOW_SEARCH = "sortShowSearch";
  public static final String SORT_SHOW_TRENDING = "sortShowTrending";
  public static final String SORT_SHOW_RECOMMENDED = "sortShowRecommended";

  public static final String SORT_MOVIE_SEARCH = "sortMovieSearch";
  public static final String SORT_MOVIE_TRENDING = "sortMovieTrending";
  public static final String SORT_MOVIE_RECOMMENDED = "sortMovieRecommended";

  // User settings returned by account/settings
  public static final String PROFILE_USERNAME = "profileUsername";
  public static final String PROFILE_FULL_NAME = "profileFullName";
  public static final String PROFILE_GENDER = "profileGender";
  public static final String PROFILE_AGE = "profileAge";
  public static final String PROFILE_LOCATION = "profileLocation";
  public static final String PROFILE_ABOUT = "profileAbout";
  public static final String PROFILE_JOINED = "profileJoined";
  public static final String PROFILE_AVATAR = "profileAvatar";
  public static final String PROFILE_PRIVATE = "profilePrivate";
  public static final String PROFILE_VIP = "profileVip";

  public static final String PROFILE_TIMEZONE = "profileTimezone";
  public static final String PROFILE_COVER_IMAGE = "profileCoverImage";

  public static final String PROFILE_CONNECTION_FACEBOOK = "profileConnectionFacebook";
  public static final String PROFILE_CONNECTION_TWITTER = "profileConnectionTwitter";
  public static final String PROFILE_CONNECTION_TUMBLR = "profileConnectionTumblr";

  public static final String PROFILE_SHARING_TEXT_WATCHING = "profileSharingTextWatching";
  public static final String PROFILE_SHARING_TEXT_WATCHED = "profileSharingTextWatched";

  public static boolean isLoggedIn(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    return settings.getBoolean(TRAKT_LOGGED_IN, false);
  }

  public static void updateProfile(Context context, UserSettings userSettings) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    Profile profile = userSettings.getUser();
    if (profile != null) {
      putString(editor, Settings.PROFILE_USERNAME, profile.getUsername());
      putString(editor, Settings.PROFILE_FULL_NAME, profile.getName());
      putString(editor, Settings.PROFILE_GENDER, profile.getGender());
      putInt(editor, Settings.PROFILE_AGE, profile.getAge());
      putString(editor, Settings.PROFILE_LOCATION, profile.getLocation());
      putString(editor, Settings.PROFILE_ABOUT, profile.getAbout());
      putLong(editor, Settings.PROFILE_JOINED, profile.getJoinedAt().getTimeInMillis());

      putBoolean(editor, Settings.PROFILE_PRIVATE, profile.isPrivate());
      putBoolean(editor, Settings.PROFILE_VIP, profile.isVip());

      Images images = profile.getImages();
      putString(editor, Settings.PROFILE_AVATAR, images.getAvatar().getFull());
    }

    Account account = userSettings.getAccount();
    if (account != null) {
      putString(editor, Settings.PROFILE_TIMEZONE, account.getTimezone());
      putString(editor, Settings.PROFILE_COVER_IMAGE, account.getCoverImage());
    }

    Connections connection = userSettings.getConnections();
    if (connection != null) {
      putBoolean(editor, Settings.PROFILE_CONNECTION_FACEBOOK, connection.getFacebook());
      putBoolean(editor, Settings.PROFILE_CONNECTION_TWITTER, connection.getTwitter());
      putBoolean(editor, Settings.PROFILE_CONNECTION_TUMBLR, connection.getTumblr());
    }

    SharingText sharingText = userSettings.getSharingText();
    if (sharingText != null) {
      putString(editor, Settings.PROFILE_SHARING_TEXT_WATCHING, sharingText.getWatching());
      putString(editor, Settings.PROFILE_SHARING_TEXT_WATCHED, sharingText.getWatched());
    }

    editor.commit();
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

    editor.remove(PROFILE_USERNAME);
    editor.remove(PROFILE_FULL_NAME);
    editor.remove(PROFILE_GENDER);
    editor.remove(PROFILE_AGE);
    editor.remove(PROFILE_LOCATION);
    editor.remove(PROFILE_ABOUT);
    editor.remove(PROFILE_JOINED);
    editor.remove(PROFILE_AVATAR);
    editor.remove(PROFILE_PRIVATE);
    editor.remove(PROFILE_VIP);
    editor.remove(PROFILE_TIMEZONE);
    editor.remove(PROFILE_COVER_IMAGE);
    editor.remove(PROFILE_CONNECTION_FACEBOOK);
    editor.remove(PROFILE_CONNECTION_TWITTER);
    editor.remove(PROFILE_CONNECTION_TUMBLR);
    editor.remove(PROFILE_SHARING_TEXT_WATCHING);
    editor.remove(PROFILE_SHARING_TEXT_WATCHED);

    editor.apply();
  }
}
