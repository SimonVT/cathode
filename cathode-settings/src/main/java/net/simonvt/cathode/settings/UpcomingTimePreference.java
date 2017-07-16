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
import android.preference.PreferenceManager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UpcomingTimePreference {

  public interface UpcomingTimeChangeListener {

    void onUpcomingTimeChanged(UpcomingTime upcomingTime);
  }

  private static UpcomingTimePreference instance;

  private Context context;
  private SharedPreferences settings;

  private final List<WeakReference<UpcomingTimeChangeListener>> listeners = new ArrayList<>();

  public static void init(Context context) {
    if (instance == null) {
      instance = new UpcomingTimePreference(context);
    }
  }

  public static UpcomingTimePreference getInstance() {
    if (instance == null) {
      throw new IllegalStateException("UpcomingTimePreference not initialized");
    }

    return instance;
  }

  private UpcomingTimePreference(Context context) {
    this.context = context;
    settings = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public void set(UpcomingTime upcomingTime) {
    settings.edit().putLong(Settings.UPCOMING_TIME, upcomingTime.getCacheTime()).apply();
    onUpcomingTimeChanged(upcomingTime);
  }

  public UpcomingTime get() {
    return UpcomingTime.fromValue(
        settings.getLong(Settings.UPCOMING_TIME, UpcomingTime.WEEKS_1.getCacheTime()));
  }

  public void registerListener(UpcomingTimeChangeListener listener) {
    listeners.add(new WeakReference<>(listener));
  }

  public void unregisterListener(UpcomingTimeChangeListener listener) {
    for (int i = listeners.size() - 1; i >= 0; i--) {
      WeakReference<UpcomingTimeChangeListener> ref = listeners.get(i);
      UpcomingTimeChangeListener l = ref.get();
      if (l == null || l == listener) {
        listeners.remove(ref);
      }
    }
  }

  private void onUpcomingTimeChanged(UpcomingTime upcomingTime) {
    for (int i = listeners.size() - 1; i >= 0; i--) {
      WeakReference<UpcomingTimeChangeListener> ref = listeners.get(i);
      UpcomingTimeChangeListener l = ref.get();
      if (l == null) {
        listeners.remove(ref);
        continue;
      }

      l.onUpcomingTimeChanged(upcomingTime);
    }
  }
}
