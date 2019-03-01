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

package net.simonvt.cathode.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import butterknife.ButterKnife;
import butterknife.OnClick;
import net.simonvt.cathode.R;
import net.simonvt.cathode.settings.login.LoginActivity;
import net.simonvt.cathode.settings.setup.CalendarSetupActivity;
import net.simonvt.cathode.ui.BaseActivity;

public class StartActivity extends BaseActivity {

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.activity_start);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.yes) void connectTrakt() {
    Intent i = new Intent(this, LoginActivity.class);
    startActivity(i);
  }

  @OnClick(R.id.no) void dontConnect() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    settings.edit().putBoolean(TraktLinkSettings.TRAKT_LINK_PROMPTED, true).apply();

    Intent i = new Intent(this, CalendarSetupActivity.class);
    startActivity(i);
    finish();
  }
}
