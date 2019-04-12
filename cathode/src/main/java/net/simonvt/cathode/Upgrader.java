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
import android.os.Bundle;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.Timestamps;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.TraktTimestamps;

public final class Upgrader {

  private static final String SETTINGS_VERSION = "settingsVersion";
  private static final int VERSION = 5;

  private Upgrader() {
  }

  public static void upgrade(Context context) {
    final int currentVersion = Settings.get(context).getInt(SETTINGS_VERSION, -1);
    if (currentVersion == -1) {
      Settings.get(context)
          .edit()
          .putInt(SETTINGS_VERSION, VERSION)
          .putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE)
          .apply();
      return;
    }

    if (currentVersion < VERSION) {
      if (currentVersion < 1) {
        if (TraktLinkSettings.isLinked(context)) {
          Account account = Accounts.getAccount(context);
          ContentResolver.removePeriodicSync(account, BuildConfig.PROVIDER_AUTHORITY,
              new Bundle());
          ContentResolver.setSyncAutomatically(account, BuildConfig.PROVIDER_AUTHORITY, false);
          ContentResolver.setIsSyncable(account, BuildConfig.PROVIDER_AUTHORITY, 0);
        }
      }

      if (currentVersion < 2) {
        Settings.get(context)
            .edit()
            .remove("suggestions")
            .remove("lastSyncHidden")
            .remove("lastPurge")
            .remove("tmdbLastConfigurationUpdate")
            .apply();
      }

      if (currentVersion < 3) {
        TraktTimestamps.getSettings(context).edit().remove(TraktTimestamps.EPISODE_RATING).apply();
      }

      if (currentVersion < 4) {
        boolean linked = Settings.get(context).getBoolean(TraktLinkSettings.TRAKT_LINKED, false);
        if (linked) {
          Settings.get(context)
              .edit()
              .putBoolean(TraktLinkSettings.TRAKT_LINK_PROMPTED, true)
              .apply();
        }
      }

      if (currentVersion < 5) {
        long showsLastUpdated = Timestamps.get(context)
            .getLong(Timestamps.SHOWS_LAST_UPDATED, System.currentTimeMillis());
        long moviesLastUpdated = Timestamps.get(context)
            .getLong(Timestamps.MOVIES_LAST_UPDATED, System.currentTimeMillis());
        TraktTimestamps.clear(context);
        Timestamps.get(context)
            .edit()
            .putLong(Timestamps.SHOWS_LAST_UPDATED, showsLastUpdated)
            .putLong(Timestamps.MOVIES_LAST_UPDATED, moviesLastUpdated)
            .apply();
      }

      Settings.get(context).edit().putInt(SETTINGS_VERSION, VERSION).apply();
    }

    Settings.get(context).edit().putInt(Settings.VERSION_CODE, BuildConfig.VERSION_CODE).apply();
  }
}
