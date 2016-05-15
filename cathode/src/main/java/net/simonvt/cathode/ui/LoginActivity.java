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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.setup.CalendarSetupActivity;

public class LoginActivity extends BaseActivity implements TokenTask.Callback {

  static final String QUERY_CODE = "code";

  static final int REQUEST_OAUTH = 1;

  @Inject JobManager jobManager;

  @BindView(R.id.buttonContainer) View buttonContainer;
  @BindView(R.id.error_message) TextView errorMessage;

  @BindView(R.id.progressContainer) View progressContainer;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CathodeApp.inject(this);

    setContentView(R.layout.activity_login);
    ButterKnife.bind(this);

    if (TokenTask.runningInstance != null) {
      TokenTask.runningInstance.setCallback(this);
      setRefreshing(true);
    }
  }

  @OnClick(R.id.login) void onLoginClick() {
    Intent authorize = new Intent(LoginActivity.this, OauthWebViewActivity.class);
    startActivityForResult(authorize, REQUEST_OAUTH);
    errorMessage.setVisibility(View.GONE);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_OAUTH && resultCode == RESULT_OK) {
      final String code = data.getStringExtra(QUERY_CODE);
      TokenTask.start(this, code, this);
      setRefreshing(true);
    }
  }

  @Override public void onTokenFetched() {
    setRefreshing(false);

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    settings.edit()
        .putBoolean(Settings.TRAKT_LOGGED_IN, true)
        .putBoolean(Settings.INITIAL_SYNC, true)
        .apply();

    final String username = settings.getString(Settings.Profile.USERNAME, null);

    Accounts.setupAccount(this, username);

    Intent setup = new Intent(this, CalendarSetupActivity.class);
    startActivity(setup);
    finish();

    jobManager.addJob(new SyncJob());
  }

  @Override public void onTokenFetchedFail(int error) {
    setRefreshing(false);

    errorMessage.setVisibility(View.VISIBLE);
    errorMessage.setText(error);
  }

  void setRefreshing(boolean refreshing) {
    if (refreshing) {
      buttonContainer.setVisibility(View.GONE);
      progressContainer.setVisibility(View.VISIBLE);
    } else {
      buttonContainer.setVisibility(View.VISIBLE);
      progressContainer.setVisibility(View.GONE);
    }
  }
}
