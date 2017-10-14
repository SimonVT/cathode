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

package net.simonvt.cathode.api;

import android.content.Context;
import android.text.format.DateUtils;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.api.entity.TokenRequest;
import net.simonvt.cathode.api.enumeration.GrantType;
import net.simonvt.cathode.api.service.AuthorizationService;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.jobs.BuildConfig;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktLinkSettings;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class ApiSettings implements TraktSettings {

  private static volatile ApiSettings instance;

  public static ApiSettings getInstance(Context context) {
    if (instance == null) {
      synchronized (ApiSettings.class) {
        if (instance == null) {
          instance = new ApiSettings(context.getApplicationContext());
        }
      }
    }

    return instance;
  }

  private Context context;

  @Inject AuthorizationService authService;

  private volatile boolean refreshingToken;

  public ApiSettings(Context context) {
    this.context = context;
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

  private boolean isTokenExpired() {
    synchronized (this) {
      return Settings.get(context).getLong(TraktLinkSettings.TRAKT_TOKEN_EXPIRATION, 0)
          < System.currentTimeMillis();
    }
  }

  private boolean isRefreshingToken() {
    synchronized (this) {
      return refreshingToken;
    }
  }

  public void setRefreshingToken(boolean refreshingToken) {
    synchronized (this) {
      this.refreshingToken = refreshingToken;
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

  private void clearRefreshToken() {
    synchronized (this) {
      Settings.get(context).edit().remove(TraktLinkSettings.TRAKT_REFRESH_TOKEN).apply();
    }
  }

  @Override public String refreshToken() {
    synchronized (this) {
      if (!isRefreshingToken()) {
        if (authService == null) {
          Injector.inject(this);
        }

        if (!isTokenExpired()) {
          Timber.d("Token still valid");
          return getAccessToken();
        }

        final String refreshToken = getRefreshToken();
        if (refreshToken == null) {
          return null;
        }

        try {
          setRefreshingToken(true);

          TokenRequest tokenRequest =
              TokenRequest.refreshToken(refreshToken, getClientId(), getSecret(), getRedirectUrl(),
                  GrantType.REFRESH_TOKEN);

          Timber.d("Getting new tokens, with refresh token: %s", refreshToken);
          Call<AccessToken> call = authService.getToken(tokenRequest);
          Response<AccessToken> response = call.execute();
          if (response.isSuccessful()) {
            AccessToken token = response.body();
            updateTokens(token);
            return token.getAccessToken();
          } else {
            if (response.code() == 401) {
              clearRefreshToken();
              return null;
            }

            String message = "Code: " + response.code();
            Timber.e(new TokenRefreshFailedException(message), "Unable to get token");
          }
        } catch (IOException e) {
          Timber.d(e, "Unable to get new tokens");
        } finally {
          setRefreshingToken(false);
        }
      }
    }

    return null;
  }

  public static class TokenRefreshFailedException extends Exception {

    public TokenRefreshFailedException() {
      super();
    }

    public TokenRefreshFailedException(String detailMessage) {
      super(detailMessage);
    }

    public TokenRefreshFailedException(String detailMessage, Throwable throwable) {
      super(detailMessage, throwable);
    }

    public TokenRefreshFailedException(Throwable throwable) {
      super(throwable);
    }
  }
}
