/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote.sync.shows;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.util.DateUtils;
import timber.log.Timber;

public class StartSyncUpdatedShows extends Job {

  @Override public String key() {
    return "StartUpdatedShowsSync";
  }

  @Override public int getPriority() {
    return PRIORITY_2;
  }

  @Override public void perform() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    String lastUpdated = settings.getString(Settings.SHOWS_LAST_UPDATED, null);
    String currentTime = TimeUtils.getIsoTime();

    if (lastUpdated != null) {
      long millis = TimeUtils.getMillis(lastUpdated);
      millis = millis - 12 * DateUtils.HOUR_IN_MILLIS;
      String updatedSince = TimeUtils.getIsoTime(millis);
      Timber.i("Last updated: " + lastUpdated);
      Timber.i("Millis: " + millis);
      Timber.i("Updated since: " + updatedSince);
      if (updatedSince == null) {
        long currentTimeMillis = System.currentTimeMillis();
        currentTimeMillis = currentTimeMillis - DateUtils.DAY_IN_MILLIS;
        updatedSince = TimeUtils.getIsoTime(currentTimeMillis);
      }
      if (updatedSince == null) {
        Timber.e(new Exception("Last sync time failed"), "Last sync time failed");
        return;
      } else {
        queue(new SyncUpdatedShows(updatedSince, 1));
      }
    }

    settings.edit().putString(Settings.SHOWS_LAST_UPDATED, currentTime).apply();
  }
}
