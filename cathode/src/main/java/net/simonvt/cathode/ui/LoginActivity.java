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

package net.simonvt.cathode.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.UserToken;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.api.entity.TokenRequest;
import net.simonvt.cathode.api.entity.UserSettings;
import net.simonvt.cathode.api.enumeration.GrantType;
import net.simonvt.cathode.api.service.AuthorizationService;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.settings.Settings;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public class LoginActivity extends BaseActivity {

  static final String QUERY_CODE = "code";

  static final int REQUEST_OAUTH = 1;

  @Inject JobManager jobManager;

  @Inject Bus bus;

  @InjectView(R.id.buttonContainer) View buttonContainer;
  @InjectView(R.id.error_message) TextView errorMessage;
  @InjectView(R.id.login) Button login;

  @InjectView(R.id.progressContainer) View progressContainer;

  private boolean fetchingToken;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CathodeApp.inject(this);

    setContentView(R.layout.activity_login);
    ButterKnife.inject(this);

    login.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        Intent authorize = new Intent(LoginActivity.this, OauthWebViewActivity.class);
        startActivityForResult(authorize, REQUEST_OAUTH);
        errorMessage.setVisibility(View.GONE);
      }
    });
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_OAUTH && resultCode == RESULT_OK) {
      final String code = data.getStringExtra(QUERY_CODE);
      new TokenTask(this).execute(code);
    }
  }

  @Override protected void onResume() {
    super.onResume();
    bus.register(this);
  }

  @Override protected void onPause() {
    bus.unregister(this);
    super.onPause();
  }

  @Subscribe public void onFetchingTokenEvent(FetchingTokenEvent event) {
    if (isFinishing()) {
      Timber.d("Is finishing");
      return;
    }

    fetchingToken = event.isFetchingToken();

    if (fetchingToken) {
      buttonContainer.setVisibility(View.GONE);
      progressContainer.setVisibility(View.VISIBLE);
    } else {
      buttonContainer.setVisibility(View.VISIBLE);
      progressContainer.setVisibility(View.GONE);
    }
  }

  @Subscribe public void onTokenFetched(TokenFetchedEvent event) {
    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this) //
        .edit() //
        .putBoolean(Settings.TRAKT_LOGGED_IN, true) //
        .putString(Settings.TRAKT_TOKEN, event.getToken()) //
        .apply();

    Intent home = new Intent(this, HomeActivity.class);
    startActivity(home);
    finish();

    jobManager.addJob(new SyncJob());
  }

  @Subscribe public void onFetchingTokenFailedEvent(FetchingTokenFailedEvent event) {
    errorMessage.setVisibility(View.VISIBLE);
    errorMessage.setText(event.getError());
  }

  private static class Result {

    boolean success;

    String token;

    int errorMessage;

    private Result(String token) {
      this.token = token;
      this.success = true;
    }

    public Result(int errorMessage) {
      this.errorMessage = errorMessage;
      this.success = false;
    }
  }

  public static class TokenTask extends AsyncTask<String, Void, Result> {

    @Inject AuthorizationService authorizationService;
    @Inject UsersService usersService;
    @Inject UserToken userToken;
    @Inject Bus bus;

    final Context context;

    public TokenTask(Context context) {
      this.context = context.getApplicationContext();
      CathodeApp.inject(context, this);
      bus.register(this);
    }

    @Produce public FetchingTokenEvent produceFetchingTokenEvent() {
      return new FetchingTokenEvent(true);
    }

    @Override protected Result doInBackground(String... params) {
      String code = params[0];

      try {
        final AccessToken token = authorizationService.getToken(
            new TokenRequest(code, BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_SECRET,
                BuildConfig.TRAKT_REDIRECT_URL, GrantType.AUTHORIZATION_CODE));

        final String accessToken = token.getAccessToken();

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putString(Settings.TRAKT_TOKEN, accessToken).apply();

        userToken.setToken(accessToken);

        final UserSettings userSettings = usersService.getUserSettings();
        Settings.clearProfile(context);
        Settings.updateProfile(context, userSettings);

        return new Result(accessToken);
      } catch (RetrofitError e) {
        switch (e.getKind()) {
          case NETWORK:
            return new Result(R.string.error_network);
          case HTTP:
            Response response = e.getResponse();
            if (response != null) {
              int status = response.getStatus();
              if (status >= 500 && status < 600) {
                return new Result(R.string.login_error_5xx);
              }
            }
          case CONVERSION:
          case UNEXPECTED:
        }

        Timber.e(e, "Unable to get token");
        return new Result(R.string.login_error_unknown);
      }
    }

    @Override protected void onPostExecute(Result result) {
      bus.unregister(this);

      if (result.success) {
        bus.post(new TokenFetchedEvent(result.token));
      } else {
        bus.post(new FetchingTokenFailedEvent(result.errorMessage));
      }

      bus.post(new FetchingTokenEvent(false));
    }
  }

  public static class FetchingTokenEvent {

    private boolean fetchingToken;

    public FetchingTokenEvent(boolean fetchingToken) {
      this.fetchingToken = fetchingToken;
    }

    public boolean isFetchingToken() {
      return fetchingToken;
    }
  }

  public static class TokenFetchedEvent {

    private String token;

    public TokenFetchedEvent(String token) {
      this.token = token;
    }

    public String getToken() {
      return token;
    }
  }

  public static class FetchingTokenFailedEvent {

    private int error;

    public FetchingTokenFailedEvent(int error) {
      this.error = error;
    }

    public int getError() {
      return error;
    }
  }
}
