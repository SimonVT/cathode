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

package net.simonvt.cathode.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class Timestamps {

  private static final String SETTINGS_FILE = "cathode_timestamps";

  public static final String LAST_CONFIG_SYNC = "configSync";
  public static final String SHOWS_LAST_UPDATED = "showsLastUpdated";
  public static final String MOVIES_LAST_UPDATED = "moviesLastUpdated";

  private Timestamps() {
  }

  public static SharedPreferences get(Context context) {
    return context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
  }

  public static void clear(Context context) {
    get(context).edit().clear().apply();
  }
}
