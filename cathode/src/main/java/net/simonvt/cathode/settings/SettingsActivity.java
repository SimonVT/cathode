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

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import dagger.android.AndroidInjection;
import javax.inject.Inject;
import net.simonvt.android.colorpicker.ColorPickerDialog;
import net.simonvt.android.colorpicker.ColorPickerSwatch;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.Adapters;
import net.simonvt.cathode.common.util.VersionCodes;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.settings.hidden.HiddenItems;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;
import timber.log.Timber;

public class SettingsActivity extends BaseActivity {

  private static final String FRAGMENT_SETTINGS =
      "net.simonvt.cathode.settings.SettingsActivity.settingsFragment";

  private static final String DIALOG_COLOR_PICKER =
      "net.simonvt.cathode.settings.SettingsActivity.ColorPicker";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_toolbar);
    if (getFragmentManager().findFragmentByTag(FRAGMENT_SETTINGS) == null) {
      SettingsFragment settings = new SettingsFragment();
      getFragmentManager().beginTransaction()
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

  public static class SettingsFragment extends PreferenceFragment
      implements UpcomingTimeDialog.UpcomingTimeSelectedListener,
      ColorPickerSwatch.OnColorSelectedListener, ShowOffsetDialog.ShowOffsetSelectedListener {

    private static final String DIALOG_UPCOMING_TIME =
        "net.simonvt.cathode.settings.SettingsActivity.SettingsFragment.upcomingTime";
    private static final String DIALOG_SHOW_OFFSET =
        "net.simonvt.cathode.settings.SettingsActivity.SettingsFragment.showOffset";

    private static final int PERMISSION_REQUEST_CALENDAR = 11;

    @Inject UpcomingTimePreference upcomingTimePreference;

    SwitchPreference syncCalendar;

    private boolean isTablet;

    @Override public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      AndroidInjection.inject(this);

      isTablet = getResources().getBoolean(R.bool.isTablet);

      addPreferencesFromResource(R.xml.settings_general);

      syncCalendar = (SwitchPreference) findPreference(Settings.CALENDAR_SYNC);
      syncCalendar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
          boolean checked = (boolean) newValue;
          syncCalendar.setChecked(checked);

          if (VersionCodes.isAtLeastM()) {
            if (checked && !Permissions.hasCalendarPermission(getActivity())) {
              requestPermission();
            }
          } else {
            Accounts.requestCalendarSync(getActivity());
          }

          return true;
        }
      });

      findPreference("calendarColor").setOnPreferenceClickListener(
          new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
              final int calendarColor = Settings.get(getActivity())
                  .getInt(Settings.CALENDAR_COLOR, Settings.CALENDAR_COLOR_DEFAULT);
              final int size =
                  isTablet ? ColorPickerDialog.SIZE_LARGE : ColorPickerDialog.SIZE_SMALL;
              ColorPickerDialog dialog =
                  ColorPickerDialog.newInstance(R.string.preference_calendar_color,
                      Settings.CALENDAR_COLORS, calendarColor, 5, size);
              dialog.setTargetFragment(SettingsFragment.this, 0);
              dialog.show(getFragmentManager(), DIALOG_COLOR_PICKER);

              if (Settings.get(getActivity()).getBoolean(Settings.CALENDAR_SYNC, false)) {
                Accounts.requestCalendarSync(getActivity());
              }

              return true;
            }
          });

      findPreference("notifications").setOnPreferenceClickListener(
          new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
              startActivity(new Intent(getActivity(), NotificationSettingsActivity.class));
              return true;
            }
          });

      findPreference("hiddenItems").setOnPreferenceClickListener(
          new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
              startActivity(new Intent(getActivity(), HiddenItems.class));
              return true;
            }
          });

      findPreference("upcomingTime").setOnPreferenceClickListener(
          new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
              UpcomingTime upcomingTime = UpcomingTime.fromValue(Settings.get(getActivity())
                  .getLong(Settings.UPCOMING_TIME, UpcomingTime.WEEKS_1.getCacheTime()));
              UpcomingTimeDialog dialog = UpcomingTimeDialog.newInstance(upcomingTime);
              dialog.setTargetFragment(SettingsFragment.this, 0);
              dialog.show(getFragmentManager(), DIALOG_UPCOMING_TIME);
              return true;
            }
          });

      findPreference(Settings.SHOWS_AVOID_SPOILERS).setOnPreferenceChangeListener(
          new Preference.OnPreferenceChangeListener() {
            @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
              Adapters.notifyAdapters();

              if (Settings.get(getActivity()).getBoolean(Settings.CALENDAR_SYNC, false)) {
                Accounts.requestCalendarSync(getActivity());
              }
              return true;
            }
          });

      findPreference(Settings.SHOWS_OFFSET).setOnPreferenceClickListener(
          new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
              final int offset = FirstAiredOffsetPreference.getInstance().getOffsetHours();
              ShowOffsetDialog dialog = ShowOffsetDialog.newInstance(offset);
              dialog.setTargetFragment(SettingsFragment.this, 0);
              dialog.show(getFragmentManager(), DIALOG_SHOW_OFFSET);
              return true;
            }
          });

      findPreference("about").setOnPreferenceClickListener(
          new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
              new AboutDialog().show(getFragmentManager(), HomeActivity.DIALOG_ABOUT);
              return true;
            }
          });

      findPreference("logout").setOnPreferenceClickListener(
          new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference preference) {
              new LogoutDialog().show(getFragmentManager(), HomeActivity.DIALOG_LOGOUT);
              return true;
            }
          });
    }

    @RequiresApi(Build.VERSION_CODES.M) private void requestPermission() {
      requestPermissions(new String[] {
          Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR,
      }, PERMISSION_REQUEST_CALENDAR);
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
      if (requestCode == PERMISSION_REQUEST_CALENDAR) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Timber.d("Calendar permission granted");
          Accounts.requestCalendarSync(getActivity());
        } else {
          Timber.d("Calendar permission not granted");
        }
      }
    }

    @Override public void onUpcomingTimeSelected(UpcomingTime value) {
      upcomingTimePreference.set(value);
    }

    @Override public void onColorSelected(int color) {
      final int calendarColor = Settings.get(getActivity())
          .getInt(Settings.CALENDAR_COLOR, Settings.CALENDAR_COLOR_DEFAULT);
      if (color != calendarColor) {
        Settings.get(getActivity())
            .edit()
            .putInt(Settings.CALENDAR_COLOR, color)
            .putBoolean(Settings.CALENDAR_COLOR_NEEDS_UPDATE, true)
            .apply();

        if (Permissions.hasCalendarPermission(getActivity())) {
          Accounts.requestCalendarSync(getActivity());
        }
      }
    }

    @Override public void onShowOffsetSelected(int offset) {
      final int showOffset = FirstAiredOffsetPreference.getInstance().getOffsetHours();
      if (offset != showOffset) {
        FirstAiredOffsetPreference.getInstance().set(offset);

        final Context context = getActivity();

        new Thread(new Runnable() {
          @Override public void run() {
            ContentValues values = new ContentValues();
            values.put(EpisodeColumns.LAST_MODIFIED, System.currentTimeMillis());
            context.getContentResolver().update(Episodes.EPISODES, values, null, null);

            Accounts.requestCalendarSync(context);
          }
        }).start();
      }
    }
  }
}
