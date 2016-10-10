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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

public final class TmdbTimestamps {

  private TmdbTimestamps() {
  }

  public static boolean shouldUpdateConfiguration(Context context, long lastUpdated) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    final long lastActivity = settings.getLong(Settings.TMDB_LAST_CONFIGURATION_UPDATE, -1L);
    return lastActivity == -1L || lastUpdated + 2 * DateUtils.DAY_IN_MILLIS > lastActivity;
  }

  public static void clear(Context context) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();

    editor.remove(Settings.TMDB_LAST_CONFIGURATION_UPDATE);

    editor.apply();
  }
}
