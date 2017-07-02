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

package net.simonvt.cathode.ui.shows.upcoming;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.common.util.MainHandler;

public class UpcomingSortByPreference {

  public interface UpcomingSortByListener {

    void onUpcomingSortByChanged(UpcomingSortBy sortBy);
  }

  private static UpcomingSortByPreference instance;

  private Context context;
  private SharedPreferences settings;

  private final List<WeakReference<UpcomingSortByListener>> listeners = new ArrayList<>();

  public static void init(Context context) {
    if (instance == null) {
      instance = new UpcomingSortByPreference(context);
    }
  }

  public static UpcomingSortByPreference getInstance() {
    if (instance == null) {
      throw new IllegalStateException("UpcomingSortByPreference not initialized");
    }

    return instance;
  }

  private UpcomingSortByPreference(Context context) {
    this.context = context;
    settings = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public void set(UpcomingSortBy sortBy) {
    settings.edit().putString(Settings.Sort.SHOW_UPCOMING, sortBy.getKey()).apply();
    post(sortBy);
  }

  public UpcomingSortBy get() {
    return UpcomingSortBy.fromValue(
        settings.getString(Settings.Sort.SHOW_UPCOMING, UpcomingSortBy.NEXT_EPISODE.getKey()));
  }

  public void registerListener(UpcomingSortByListener listener) {
    synchronized (listeners) {
      listeners.add(new WeakReference<>(listener));
    }
  }

  public void unregisterListener(UpcomingSortByListener listener) {
    synchronized (listeners) {
      for (int i = listeners.size() - 1; i >= 0; i--) {
        WeakReference<UpcomingSortByListener> ref = listeners.get(i);
        UpcomingSortByListener l = ref.get();
        if (l == null || l == listener) {
          listeners.remove(ref);
        }
      }
    }
  }

  public void post(final UpcomingSortBy sortBy) {
    MainHandler.post(new Runnable() {
      @Override public void run() {
        synchronized (listeners) {
          for (int i = listeners.size() - 1; i >= 0; i--) {
            WeakReference<UpcomingSortByListener> ref = listeners.get(i);
            UpcomingSortByListener l = ref.get();
            if (l == null) {
              listeners.remove(ref);
              continue;
            }

            l.onUpcomingSortByChanged(sortBy);
          }
        }
      }
    });
  }
}
