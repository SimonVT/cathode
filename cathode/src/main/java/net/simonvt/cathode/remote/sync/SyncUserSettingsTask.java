/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.remote.sync;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Account;
import net.simonvt.cathode.api.entity.Connections;
import net.simonvt.cathode.api.entity.Profile;
import net.simonvt.cathode.api.entity.SharingText;
import net.simonvt.cathode.api.entity.UserSettings;
import net.simonvt.cathode.api.entity.Viewing;
import net.simonvt.cathode.api.entity.Viewing.Ratings;
import net.simonvt.cathode.api.entity.Viewing.Shouts;
import net.simonvt.cathode.api.service.AccountService;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.Settings;

public class SyncUserSettingsTask extends TraktTask {

  @Inject AccountService accountService;

  @Override protected void doTask() {
    UserSettings userSettings = accountService.settings();

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    Editor editor = settings.edit();

    Profile profile = userSettings.getProfile();
    if (profile != null) {
      putString(editor, Settings.PROFILE_FULL_NAME, profile.getFullName());
      putString(editor, Settings.PROFILE_GENDER, profile.getGender());
      putInt(editor, Settings.PROFILE_AGE, profile.getAge());
      putString(editor, Settings.PROFILE_LOCATION, profile.getLocation());
      putString(editor, Settings.PROFILE_ABOUT, profile.getAbout());
      putLong(editor, Settings.PROFILE_JOINED, profile.getJoined());
      putLong(editor, Settings.PROFILE_LAST_LOGIN, profile.getLastLogin());
      putString(editor, Settings.PROFILE_AVATAR, profile.getAvatar());
      putString(editor, Settings.PROFILE_URL, profile.getUrl());
      putBoolean(editor, Settings.PROFILE_VIP, profile.isVip());
    }

    Account account = userSettings.getAccount();
    if (account != null) {
      putString(editor, Settings.PROFILE_TIMEZONE, account.getTimezone());
      putBoolean(editor, Settings.PROFILE_USE_24_HOUR, account.use24hr());
      putBoolean(editor, Settings.PROFILE_PROTECTED, account.isProtected());
    }

    Viewing viewing = userSettings.getViewing();
    if (viewing != null) {
      Ratings ratings = viewing.getRatings();
      putString(editor, Settings.PROFILE_RATING_MODE, ratings.getMode().toString());
      Shouts shouts = viewing.getShouts();
      putBoolean(editor, Settings.PROFILE_SHOUTS_SHOW_BADGES, shouts.getShowBadges());
      putBoolean(editor, Settings.PROFILE_SHOUTS_SHOW_SPOILERS, shouts.getShowSpoilers());
    }

    Connections connection = userSettings.getConnections();
    if (connection != null) {
      putBoolean(editor, Settings.PROFILE_CONNECTION_FACEBOOK, connection.getFacebook().isConnected());
      putBoolean(editor, Settings.PROFILE_CONNECTION_TWITTER, connection.getTwitter().isConnected());
      putBoolean(editor, Settings.PROFILE_CONNECTION_TUMBLR, connection.getTumblr().isConnected());
      putBoolean(editor, Settings.PROFILE_CONNECTION_PATH, connection.getPath().isConnected());
      putBoolean(editor, Settings.PROFILE_CONNECTION_PROWL, connection.getProwl().isConnected());
    }

    SharingText sharingText = userSettings.getSharingText();
    if (sharingText != null) {
      putString(editor, Settings.PROFILE_SHARING_TEXT_WATCHING, sharingText.getWatching());
      putString(editor, Settings.PROFILE_SHARING_TEXT_WATCHED, sharingText.getWatched());
    }

    editor.commit();
    postOnSuccess();
  }

  private void putString(Editor editor, String key, Object value) {
    if (value != null) {
      editor.putString(key, value.toString());
    } else {
      editor.remove(key);
    }
  }

  private void putInt(Editor editor, String key, Integer value) {
    if (value != null) {
      editor.putInt(key, value);
    } else {
      editor.remove(key);
    }
  }

  private void putLong(Editor editor, String key, Long value) {
    if (value != null) {
      editor.putLong(key, value);
    } else {
      editor.remove(key);
    }
  }

  private void putBoolean(Editor editor, String key, Boolean value) {
    if (value != null) {
      editor.putBoolean(key, value);
    } else {
      editor.remove(key);
    }
  }
}
