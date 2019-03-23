/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
import net.simonvt.cathode.api.entity.Account;
import net.simonvt.cathode.api.entity.Connections;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.SharingText;
import net.simonvt.cathode.api.entity.UserSettings;

public class ProfileSettings {

  private static final String SETTINGS_FILE = "cathode_profile";

  // User settings returned by account/settings
  public static final String USERNAME = "profileUsername";
  public static final String FULL_NAME = "profileFullName";
  public static final String GENDER = "profileGender";
  public static final String AGE = "profileAge";
  public static final String LOCATION = "profileLocation";
  public static final String ABOUT = "profileAbout";
  public static final String JOINED = "profileJoined";
  public static final String AVATAR = "profileAvatar";
  public static final String PRIVATE = "profilePrivate";
  public static final String VIP = "profileVip";
  public static final String TIMEZONE = "profileTimezone";
  public static final String COVER_IMAGE = "profileCoverImage";

  public static final String CONNECTION_FACEBOOK = "profileConnectionFacebook";
  public static final String CONNECTION_TWITTER = "profileConnectionTwitter";
  public static final String CONNECTION_TUMBLR = "profileConnectionTumblr";

  public static final String SHARING_TEXT_WATCHING = "profileSharingTextWatching";
  public static final String SHARING_TEXT_WATCHED = "profileSharingTextWatched";

  private ProfileSettings() {
  }

  public static SharedPreferences get(Context context) {
    return context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
  }

  public static void updateProfile(Context context, UserSettings userSettings) {
    SharedPreferences.Editor editor = get(context).edit();

    net.simonvt.cathode.api.entity.Profile profile = userSettings.getUser();
    if (profile != null) {
      putString(editor, USERNAME, profile.getUsername());
      putString(editor, FULL_NAME, profile.getName());
      putString(editor, GENDER, profile.getGender());
      putInt(editor, AGE, profile.getAge());
      putString(editor, LOCATION, profile.getLocation());
      putString(editor, ABOUT, profile.getAbout());
      putLong(editor, JOINED, profile.getJoined_at().getTimeInMillis());

      putBoolean(editor, PRIVATE, profile.isPrivate());
      putBoolean(editor, VIP, profile.getVip());

      Images images = profile.getImages();
      putString(editor, AVATAR, images.getAvatar().getFull());
    }

    Account account = userSettings.getAccount();
    if (account != null) {
      putString(editor, TIMEZONE, account.getTimezone());
      putString(editor, COVER_IMAGE, account.getCover_image());
    }

    Connections connection = userSettings.getConnections();
    if (connection != null) {
      putBoolean(editor, CONNECTION_FACEBOOK, connection.getFacebook());
      putBoolean(editor, CONNECTION_TWITTER, connection.getTwitter());
      putBoolean(editor, CONNECTION_TUMBLR, connection.getTumblr());
    }

    SharingText sharingText = userSettings.getSharing_text();
    if (sharingText != null) {
      putString(editor, SHARING_TEXT_WATCHING, sharingText.getWatching());
      putString(editor, SHARING_TEXT_WATCHED, sharingText.getWatched());
    }

    editor.apply();
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
    SharedPreferences.Editor editor = get(context).edit();

    editor.remove(USERNAME);
    editor.remove(FULL_NAME);
    editor.remove(GENDER);
    editor.remove(AGE);
    editor.remove(LOCATION);
    editor.remove(ABOUT);
    editor.remove(JOINED);
    editor.remove(AVATAR);
    editor.remove(PRIVATE);
    editor.remove(VIP);
    editor.remove(TIMEZONE);
    editor.remove(COVER_IMAGE);
    editor.remove(CONNECTION_FACEBOOK);
    editor.remove(CONNECTION_TWITTER);
    editor.remove(CONNECTION_TUMBLR);
    editor.remove(SHARING_TEXT_WATCHING);
    editor.remove(SHARING_TEXT_WATCHED);

    editor.apply();
  }
}
