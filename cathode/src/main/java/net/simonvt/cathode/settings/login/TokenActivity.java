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
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.settings.setup.CalendarSetupActivity;

public class TokenActivity extends BaseActivity implements TokenTask.Callback {

  static final String EXTRA_CODE = "code";

  @Inject JobManager jobManager;

  @BindView(R.id.buttonContainer) View buttonContainer;
  @BindView(R.id.error_message) TextView errorMessage;

  @BindView(R.id.progressContainer) View progressContainer;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.activity_login_token);
    ButterKnife.bind(this);
    CathodeApp.inject(this);

    if (TokenTask.runningInstance != null) {
      TokenTask.runningInstance.setCallback(this);
    } else {
      final String code = getIntent().getStringExtra(EXTRA_CODE);
      TokenTask.start(this, code, this);
    }

    setRefreshing(true);
  }

  @OnClick(R.id.retry) void onRetryClicked() {
    Intent login = new Intent(this, LoginActivity.class);
    startActivity(login);
    finish();
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

  @Override public void onTokenFetched() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    settings.edit()
        .putBoolean(Settings.TRAKT_LOGGED_IN, true)
        .putBoolean(Settings.INITIAL_SYNC, true)
        .apply();

    final String username = settings.getString(Settings.Profile.USERNAME, null);

    Accounts.setupAccount(this, username);

    jobManager.addJob(new SyncJob());

    Intent setup = new Intent(this, CalendarSetupActivity.class);
    startActivity(setup);
    finish();
  }

  @Override public void onTokenFetchedFail(int error) {
    setRefreshing(false);

    errorMessage.setVisibility(View.VISIBLE);
    errorMessage.setText(error);
  }
}
