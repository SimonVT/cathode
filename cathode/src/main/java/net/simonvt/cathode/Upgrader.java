/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.remote.ForceUpdateJob;
import net.simonvt.cathode.remote.UpdateShowCounts;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.TraktTimestamps;

public final class Upgrader {

  private static final String SETTINGS_VERSION = "settingsVersion";
  private static final int VERSION = 1;

  public interface JobQueue {

    void add(Job job);
  }

  private Upgrader() {
  }

  public static void upgrade(Context context, final JobQueue jobQueue) {
    legacyUpgrade(context, jobQueue);

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    final int currentVersion = settings.getInt(SETTINGS_VERSION, -1);
    if (currentVersion == -1) {
      settings.edit()
          .putInt(SETTINGS_VERSION, VERSION)
          .putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE)
          .apply();
      return;
    }

    if (currentVersion < VERSION) {
      if (currentVersion < 1) {
        if (Jobs.usesScheduler()) {
          if (TraktLinkSettings.isLinked(context)) {
            Account account = Accounts.getAccount(context);
            ContentResolver.removePeriodicSync(account, BuildConfig.PROVIDER_AUTHORITY,
                new Bundle());
            ContentResolver.setSyncAutomatically(account, BuildConfig.PROVIDER_AUTHORITY, false);
            ContentResolver.setIsSyncable(account, BuildConfig.PROVIDER_AUTHORITY, 0);
          }
        }

        final String showsLastUpdated = settings.getString(Settings.SHOWS_LAST_UPDATED, null);
        if (showsLastUpdated != null) {
          final long showsLastUpdatedMillis = TimeUtils.getMillis(showsLastUpdated);
          settings.edit().putLong(Settings.SHOWS_LAST_UPDATED, showsLastUpdatedMillis).apply();
        }
        final String moviesLastUpdated = settings.getString(Settings.MOVIES_LAST_UPDATED, null);
        if (moviesLastUpdated != null) {
          final long moviesLastUpdatedMillis = TimeUtils.getMillis(moviesLastUpdated);
          settings.edit().putLong(Settings.MOVIES_LAST_UPDATED, moviesLastUpdatedMillis).apply();
        }
      }

      settings.edit().putInt(SETTINGS_VERSION, VERSION).apply();
    }

    settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
  }

  public static void legacyUpgrade(Context context, final JobQueue jobQueue) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    final int currentVersion = settings.getInt(Settings.VERSION_CODE, -1);
    if (currentVersion == -1) {
      settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
      return;
    }

    if (currentVersion != BuildConfig.VERSION_CODE) {
      if (currentVersion < 20002) {
        Accounts.removeAccount(context);
        settings.edit().clear().apply();
      }
      if (currentVersion < 20501) {
        TraktTimestamps.clear(context);
      }
      if (currentVersion < 21001) {
        jobQueue.add(new ForceUpdateJob());
      }
      if (currentVersion <= 21001) {
        jobQueue.add(new UpdateShowCounts());
      }
      if (currentVersion <= 31001) {
        Account account = Accounts.getAccount(context);
        ContentResolver.setIsSyncable(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, 1);
        ContentResolver.setSyncAutomatically(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, true);
        ContentResolver.addPeriodicSync(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, new Bundle(),
            12 * 60 * 60 /* 12 hours in seconds */);

        Accounts.requestCalendarSync(context);
      }
      if (currentVersion <= 37000) {
        settings.edit().remove(Settings.START_PAGE).apply();
      }
      if (currentVersion <= 50303) {
        settings.edit().putLong(SETTINGS_VERSION, 0).apply();
        settings.edit().remove("showHidden").apply();
      }

      settings.edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
    }
  }
}
