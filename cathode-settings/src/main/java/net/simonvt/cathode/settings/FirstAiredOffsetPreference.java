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
import android.text.format.DateUtils;

public class FirstAiredOffsetPreference {

  private static FirstAiredOffsetPreference instance;

  private Context context;

  public static void init(Context context) {
    if (instance == null) {
      instance = new FirstAiredOffsetPreference(context.getApplicationContext());
    }
  }

  public static FirstAiredOffsetPreference getInstance() {
    if (instance == null) {
      throw new IllegalStateException("FirstAiredOffsetPreference not initialized");
    }

    return instance;
  }

  private FirstAiredOffsetPreference(Context context) {
    this.context = context;
  }

  public void set(int offsetHours) {
    Settings.get(context).edit().putInt(Settings.SHOWS_OFFSET, offsetHours).apply();
  }

  public int getOffsetHours() {
    return Settings.get(context).getInt(Settings.SHOWS_OFFSET, 0);
  }

  public long getOffsetMillis() {
    return getOffsetHours() * DateUtils.HOUR_IN_MILLIS;
  }
}
