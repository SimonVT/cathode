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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.widget.Toolbar;
import net.simonvt.cathode.R;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.Settings;

public class SettingsActivity extends BaseActivity {

  private static final String FRAGMENT_SETTINGS =
      "net.simonvt.cathode.ui.SettingsActivity.settingsFragment";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_toolbar);
    if (getFragmentManager().findFragmentByTag(FRAGMENT_SETTINGS) == null) {
      SettingsFragment settings = new SettingsFragment();
      getFragmentManager().beginTransaction()
          .add(R.id.content, settings, FRAGMENT_SETTINGS)
          .commit();
    }

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle("Settings"); // TODO
  }

  public static class SettingsFragment extends PreferenceFragment {

    SharedPreferences settings;

    SwitchPreference syncCalendar;

    @Override public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

      addPreferencesFromResource(R.xml.preferences_settings);

      syncCalendar = (SwitchPreference) findPreference(Settings.CALENDAR_SYNC);
      syncCalendar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
          boolean checked = (boolean) newValue;
          syncCalendar.setChecked(checked);

          Accounts.requestCalendarSync(getActivity());

          return true;
        }
      });
    }
  }
}
