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
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.TraktSettings;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.api.service.AuthorizationService;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.ProfileSettings;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.Timestamps;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.link.TraktLinkActivity;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.work.PeriodicWorkInitializer;

public class TokenActivity extends BaseActivity implements TokenTask.Callback {

  static final String EXTRA_CODE = "code";

  @Inject PeriodicWorkInitializer periodicWorkInitializer;
  @Inject JobManager jobManager;
  @Inject TraktSettings traktSettings;

  @Inject AuthorizationService authorizationService;
  @Inject UsersService usersService;

  @BindView(R.id.buttonContainer) View buttonContainer;
  @BindView(R.id.error_message) TextView errorMessage;

  @BindView(R.id.progressContainer) View progressContainer;

  private int task;

  @Override protected void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    AndroidInjection.inject(this);
    Intent intent = getIntent();
    task = intent.getIntExtra(LoginActivity.EXTRA_TASK, LoginActivity.TASK_LOGIN);

    setContentView(R.layout.activity_login_token);
    ButterKnife.bind(this);

    if (TokenTask.runningInstance != null) {
      TokenTask.runningInstance.setCallback(this);
    } else {
      final String code = getIntent().getStringExtra(EXTRA_CODE);
      TokenTask.start(code, authorizationService, usersService, traktSettings, this);
    }

    setRefreshing(true);
  }

  @OnClick(R.id.retry) void onRetryClicked() {
    Intent login = new Intent(this, LoginActivity.class);
    login.putExtra(LoginActivity.EXTRA_TASK, task);
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

  @Override public void onTokenFetched(AccessToken accessToken) {
    final boolean wasLinked = Settings.get(this).getBoolean(TraktLinkSettings.TRAKT_LINKED, false);

    ProfileSettings.clearProfile(this);
    Timestamps.get(this).edit().remove(Timestamps.LAST_CONFIG_SYNC).apply();

    Accounts.setupAccount(this);

    jobManager.addJob(new SyncJob());
    periodicWorkInitializer.initAuthWork();

    if (task == LoginActivity.TASK_LINK) {
      Intent traktSync = new Intent(this, TraktLinkActivity.class);
      startActivity(traktSync);
    } else {
      Settings.get(this)
          .edit()
          .putBoolean(TraktLinkSettings.TRAKT_LINK_PROMPTED, true)
          .putBoolean(TraktLinkSettings.TRAKT_LINKED, true)
          .putBoolean(TraktLinkSettings.TRAKT_AUTH_FAILED, false)
          .apply();

        Intent home = new Intent(this, HomeActivity.class);
        startActivity(home);
    }

    finish();
  }

  @Override public void onTokenFetchedFail(int error) {
    setRefreshing(false);

    errorMessage.setVisibility(View.VISIBLE);
    errorMessage.setText(error);
  }
}
