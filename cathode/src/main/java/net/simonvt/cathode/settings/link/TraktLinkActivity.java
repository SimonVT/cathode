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

package net.simonvt.cathode.settings.link;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.work.WorkManager;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.work.WorkManagerUtils;
import net.simonvt.cathode.work.user.PeriodicSyncWorker;
import net.simonvt.cathode.work.user.SyncUserSettingsWorker;

public class TraktLinkActivity extends BaseActivity {

  @Inject WorkManager workManager;

  private SharedPreferences settings;

  @Override protected void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    AndroidInjection.inject(this);
    settings = PreferenceManager.getDefaultSharedPreferences(this);

    setContentView(R.layout.activity_trakt_link);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.sync) void sync() {
    Intent i = new Intent(this, TraktLinkSyncActivity.class);
    startActivity(i);
  }

  @OnClick(R.id.forget) void forget() {
    TraktTimestamps.clear(this);

    settings.edit()
        .putBoolean(TraktLinkSettings.TRAKT_LINKED, true)
        .putBoolean(TraktLinkSettings.TRAKT_LINK_PROMPTED, true)
        .putBoolean(TraktLinkSettings.TRAKT_AUTH_FAILED, false)
        .apply();

    WorkManagerUtils.enqueueNow(workManager, SyncUserSettingsWorker.class);
    WorkManagerUtils.enqueueNow(workManager, PeriodicSyncWorker.class);

    Intent i = new Intent(this, HomeActivity.class);
    startActivity(i);
    finish();
  }
}
