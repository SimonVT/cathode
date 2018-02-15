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
package net.simonvt.cathode.settings.setup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import butterknife.ButterKnife;
import butterknife.OnClick;
import net.simonvt.cathode.R;
import net.simonvt.cathode.settings.Permissions;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;
import timber.log.Timber;

public class CalendarSetupActivity extends BaseActivity {

  private static final int PERMISSION_REQUEST_CALENDAR = 11;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.setup_calendar);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.yes) void syncCalendar() {
    if (!Permissions.hasCalendarPermission(this)) {
      ActivityCompat.requestPermissions(this, new String[] {
          Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR,
      }, PERMISSION_REQUEST_CALENDAR);
    } else {
      doSync();
    }
  }

  void doSync() {
    Timber.d("doSync");
    Settings.get(this).edit().putBoolean(Settings.CALENDAR_SYNC, true).apply();
    toNotifications();
  }

  @OnClick(R.id.no) void dontSync() {
    Timber.d("dontSync");
    Settings.get(this).edit().putBoolean(Settings.CALENDAR_SYNC, false).apply();
    toNotifications();
  }

  @Override public void onBackPressed() {
    Settings.get(this).edit().putBoolean(Settings.CALENDAR_SYNC, false).apply();
    toHome();
  }

  private void toHome() {
    Intent i = new Intent(this, HomeActivity.class);
    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(i);
    finish();
  }

  private void toNotifications() {
    Intent i = new Intent(this, NotificationSetupActivity.class);
    startActivity(i);
    finish();
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == PERMISSION_REQUEST_CALENDAR) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Timber.d("Calendar permission granted");
        doSync();
      } else {
        Timber.d("Calendar permission not granted");
        dontSync();
      }
    }
  }
}
