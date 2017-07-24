/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.settings.login;

import android.os.AsyncTask;
import java.io.IOException;
import java.lang.ref.WeakReference;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.TraktSettings;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.api.entity.TokenRequest;
import net.simonvt.cathode.api.enumeration.GrantType;
import net.simonvt.cathode.api.service.AuthorizationService;
import net.simonvt.cathode.api.service.UsersService;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class TokenTask extends AsyncTask<Void, Void, TokenTask.Result> {

  static class Result {

    boolean success;

    int errorMessage;

    AccessToken accessToken;

    private Result(AccessToken accessToken) {
      this.success = true;
      this.accessToken = accessToken;
    }

    private Result(int errorMessage) {
      this.errorMessage = errorMessage;
      this.success = false;
    }

    static Result success(AccessToken accessToken) {
      return new Result(accessToken);
    }

    static Result error(int errorMessage) {
      return new Result(errorMessage);
    }
  }

  public interface Callback {

    void onTokenFetched(AccessToken accessToken);

    void onTokenFetchedFail(int error);
  }

  static TokenTask runningInstance;

  AuthorizationService authorizationService;
  UsersService usersService;
  TraktSettings traktSettings;

  private String code;

  private WeakReference<Callback> callback;

  public static void start(String code, AuthorizationService authorizationService,
      UsersService usersService, TraktSettings traktSettings, Callback callback) {
    if (runningInstance != null) {
      throw new IllegalStateException("TokenTask already executing");
    }

    runningInstance = new TokenTask(code, authorizationService, usersService, traktSettings);
    runningInstance.execute();
    runningInstance.setCallback(callback);
  }

  public TokenTask(String code, AuthorizationService authorizationService,
      UsersService usersService, TraktSettings traktSettings) {
    this.code = code;
    this.authorizationService = authorizationService;
    this.usersService = usersService;
    this.traktSettings = traktSettings;
  }

  public void setCallback(Callback callback) {
    this.callback = new WeakReference<>(callback);
  }

  @Override protected TokenTask.Result doInBackground(Void... voids) {
    try {
      Call<AccessToken> call = authorizationService.getToken(
          TokenRequest.getAccessToken(code, BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_SECRET,
              BuildConfig.TRAKT_REDIRECT_URL, GrantType.AUTHORIZATION_CODE));
      Response<AccessToken> response = call.execute();

      if (response.isSuccessful()) {
        AccessToken token = response.body();
        traktSettings.updateTokens(token);

        return Result.success(token);
      } else {
        if (response.code() >= 500 && response.code() < 600) {
          return Result.error(R.string.login_error_5xx);
        }
      }
    } catch (IOException e) {
      Timber.d(e, "Unable to get token");
    }

    return Result.error(R.string.login_error_unknown);
  }

  @Override protected void onPostExecute(TokenTask.Result result) {
    runningInstance = null;

    Callback callback = this.callback.get();
    if (callback != null) {
      if (result.success) {
        callback.onTokenFetched(result.accessToken);
      } else {
        callback.onTokenFetchedFail(result.errorMessage);
      }
    }
  }
}
