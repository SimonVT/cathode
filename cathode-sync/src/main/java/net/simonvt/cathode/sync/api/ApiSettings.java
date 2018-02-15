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

package net.simonvt.cathode.sync.api;

import android.content.Context;
import android.text.format.DateUtils;
import net.simonvt.cathode.api.TraktSettings;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.sync.BuildConfig;

public class ApiSettings implements TraktSettings {

  private Context context;

  public ApiSettings(Context context) {
    this.context = context;
  }

  @Override public boolean isLinked() {
    return TraktLinkSettings.isLinked(context);
  }

  @Override public String getAccessToken() {
    synchronized (this) {
      return Settings.get(context).getString(TraktLinkSettings.TRAKT_ACCESS_TOKEN, null);
    }
  }

  @Override public String getRefreshToken() {
    synchronized (this) {
      return Settings.get(context).getString(TraktLinkSettings.TRAKT_REFRESH_TOKEN, null);
    }
  }

  @Override public boolean isTokenExpired() {
    synchronized (this) {
      return Settings.get(context).getLong(TraktLinkSettings.TRAKT_TOKEN_EXPIRATION, 0)
          < System.currentTimeMillis();
    }
  }

  @Override public void updateTokens(AccessToken tokens) {
    synchronized (this) {
      final long expirationMillis =
          System.currentTimeMillis() + (tokens.getExpiresIn() * DateUtils.SECOND_IN_MILLIS);
      Settings.get(context)
          .edit()
          .putString(TraktLinkSettings.TRAKT_ACCESS_TOKEN, tokens.getAccessToken())
          .putString(TraktLinkSettings.TRAKT_REFRESH_TOKEN, tokens.getRefreshToken())
          .putLong(TraktLinkSettings.TRAKT_TOKEN_EXPIRATION, expirationMillis)
          .apply();
    }
  }

  @Override public String getClientId() {
    return BuildConfig.TRAKT_CLIENT_ID;
  }

  @Override public String getSecret() {
    return BuildConfig.TRAKT_SECRET;
  }

  @Override public String getRedirectUrl() {
    return BuildConfig.TRAKT_REDIRECT_URL;
  }

  @Override public void clearRefreshToken() {
    synchronized (this) {
      Settings.get(context).edit().remove(TraktLinkSettings.TRAKT_REFRESH_TOKEN).apply();
    }
  }
}
