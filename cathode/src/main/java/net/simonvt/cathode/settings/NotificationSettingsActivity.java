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

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.FragmentsUtils;
import net.simonvt.cathode.notification.NotificationService;
import net.simonvt.cathode.ui.BaseActivity;

public class NotificationSettingsActivity extends BaseActivity {

  private static final String FRAGMENT_SETTINGS =
      "net.simonvt.cathode.settings.SettingsActivity.settingsFragment";

  @Override protected void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    setContentView(R.layout.activity_toolbar);
    if (getFragmentManager().findFragmentByTag(FRAGMENT_SETTINGS) == null) {
      NotificationSettingsFragment settings = new NotificationSettingsFragment();
      getSupportFragmentManager().beginTransaction()
          .add(R.id.content, settings, FRAGMENT_SETTINGS)
          .commit();
    }

    Toolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setTitle(R.string.title_settings);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        finish();
      }
    });
  }

  public static class NotificationSettingsFragment extends PreferenceFragmentCompat
      implements NotificationTimeDialog.NotificationTimeSelectedListener {

    private static final String DIALOG_NOTIFIACTION_TIME =
        "net.simonvt.cathode.settings.NotifiactionSettingsActivity.NotificationSettingsFragment.notificationTIme";

    @Override public void onCreatePreferences(@Nullable Bundle inState, String rootKey) {
      addPreferencesFromResource(R.xml.settings_notifications);

      findPreference(Settings.NOTIFICACTIONS_ENABLED).setOnPreferenceChangeListener(
          new Preference.OnPreferenceChangeListener() {
            @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
              NotificationService.start(getActivity());
              return true;
            }
          });

      findPreference(Settings.NOTIFICACTION_TIME).setOnPreferenceClickListener(
          new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
              NotificationTime notificationTime = NotificationTime.fromValue(
                  Settings.get(getActivity())
                      .getLong(Settings.NOTIFICACTION_TIME,
                          NotificationTime.HOURS_1.getNotificationTime()));
              NotificationTimeDialog dialog =
                  FragmentsUtils.instantiate(getFragmentManager(), NotificationTimeDialog.class,
                      NotificationTimeDialog.getArgs(notificationTime));
              dialog.setTargetFragment(NotificationSettingsFragment.this, 0);
              dialog.show(requireFragmentManager(), DIALOG_NOTIFIACTION_TIME);
              return true;
            }
          });
    }

    @Override public void onNotificationTimeSelected(NotificationTime value) {
      Settings.get(getActivity())
          .edit()
          .putLong(Settings.NOTIFICACTION_TIME, value.getNotificationTime())
          .apply();
      NotificationService.start(getActivity());
    }
  }
}
