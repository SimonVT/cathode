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
package net.simonvt.cathode.settings.setup;

import android.content.Intent;
import android.os.Bundle;
import butterknife.ButterKnife;
import butterknife.OnClick;
import net.simonvt.cathode.R;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;

public class NotificationSetupActivity extends BaseActivity {

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.setup_notifications);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.yes) void enableNotifications() {
    Settings.get(this).edit().putBoolean(Settings.NOTIFICACTIONS_ENABLED, true).apply();
    startHome();
  }

  @OnClick(R.id.no) void dontSync() {
    startHome();
  }

  @Override public void onBackPressed() {
    startHome();
  }

  private void startHome() {
    Intent i = new Intent(this, HomeActivity.class);
    startActivity(i);
    finish();
  }
}
