/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

import dagger.Lazy;
import java.io.IOException;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.api.entity.TokenRequest;
import net.simonvt.cathode.api.enumeration.GrantType;
import net.simonvt.cathode.api.service.AuthorizationService;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;
import timber.log.Timber;

public class TraktAuthenticator implements Authenticator {

  private TraktSettings settings;
  private Lazy<AuthorizationService> authService;

  private volatile boolean refreshingToken;

  public TraktAuthenticator(TraktSettings settings, Lazy<AuthorizationService> authService) {
    this.settings = settings;
    this.authService = authService;
  }

  @Override public Request authenticate(Route route, Response response) throws IOException {
    synchronized (TraktAuthenticator.class) {
      if (!settings.isLinked()) {
        return null;
      }

      if (responseCount(response) >= 2) {
        Timber.d("Failed 2 times, giving up");
        return null;
      }

      String credential = "Bearer " + settings.getAccessToken();
      if (credential.equals(response.request().header("Authorization"))) {
        final String refreshToken = settings.getRefreshToken();
        if (refreshToken == null) {
          Timber.d("Refresh token is null, giving up");
          return null;
        }

        final String newToken = refreshToken();
        if (newToken == null) {
          return null;
        }

        return response.request()
            .newBuilder()
            .header("Authorization", "Bearer " + newToken)
            .build();
      }

      return null;
    }
  }

  private String refreshToken() {
    synchronized (this) {
      if (!isRefreshingToken()) {
        if (!settings.isTokenExpired()) {
          Timber.d("Token still valid");
          return settings.getAccessToken();
        }

        final String refreshToken = settings.getRefreshToken();
        if (refreshToken == null) {
          return null;
        }

        try {
          setRefreshingToken(true);

          TokenRequest tokenRequest =
              TokenRequest.refreshToken(refreshToken, settings.getClientId(), settings.getSecret(),
                  settings.getRedirectUrl(), GrantType.REFRESH_TOKEN);

          Timber.d("Getting new tokens, with refresh token: %s", refreshToken);
          Call<AccessToken> call = authService.get().getToken(tokenRequest);
          retrofit2.Response<AccessToken> response = call.execute();
          if (response.isSuccessful()) {
            AccessToken token = response.body();
            settings.updateTokens(token);
            return token.getAccessToken();
          } else {
            if (response.code() == 401) {
              settings.clearRefreshToken();
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

  private int responseCount(Response response) {
    int result = 1;
    while ((response = response.priorResponse()) != null) {
      result++;
    }
    return result;
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
