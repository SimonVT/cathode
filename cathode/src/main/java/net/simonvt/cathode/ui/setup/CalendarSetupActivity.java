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

package net.simonvt.cathode.ui.setup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import butterknife.ButterKnife;
import butterknife.OnClick;
import net.simonvt.cathode.R;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;
import timber.log.Timber;

public class CalendarSetupActivity extends BaseActivity {

  private SharedPreferences settings;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.setup_calendar);
    ButterKnife.bind(this);

    settings = PreferenceManager.getDefaultSharedPreferences(this);
  }

  @OnClick(R.id.yes) void syncCalendar() {
    doSync();
  }

  void doSync() {
    Timber.d("doSync");
    settings.edit().putBoolean(Settings.CALENDAR_SYNC, true).apply();
    startHome();
  }

  @OnClick(R.id.no) void dontSync() {
    Timber.d("dontSync");
    settings.edit().putBoolean(Settings.CALENDAR_SYNC, false).apply();
    startHome();
  }

  @Override public void onBackPressed() {
    dontSync();
  }

  private void startHome() {
    Intent i = new Intent(this, HomeActivity.class);
    startActivity(i);
    finish();
  }
}
