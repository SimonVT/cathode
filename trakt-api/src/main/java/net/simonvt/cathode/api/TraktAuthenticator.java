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

import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import timber.log.Timber;

public class TraktAuthenticator implements Authenticator {

  private TraktSettings settings;

  public TraktAuthenticator(TraktSettings settings) {
    this.settings = settings;
  }

  @Override public Request authenticate(Route route, Response response) throws IOException {
    Timber.d("Auth failed");
    synchronized (TraktAuthenticator.class) {
      if (responseCount(response) >= 3) {
        Timber.d("Failed 3 times, giving up");
        return null;
      }

      String credential = "Bearer " + settings.getAccessToken();
      if (credential.equals(response.request().header("Authorization"))) {
        final String refreshToken = settings.getRefreshToken();
        if (refreshToken == null) {
          Timber.d("Refresh token is null, giving up");
          return null;
        }

        final String newToken = settings.refreshToken();
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

  private int responseCount(Response response) {
    int result = 1;
    while ((response = response.priorResponse()) != null) {
      result++;
    }
    return result;
  }
}
