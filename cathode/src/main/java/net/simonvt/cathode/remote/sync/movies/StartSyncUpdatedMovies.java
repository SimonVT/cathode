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
package net.simonvt.cathode.remote.sync.movies;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.util.DateUtils;
import timber.log.Timber;

public class StartSyncUpdatedMovies extends Job {

  @Override public String key() {
    return "StartSyncUpdatedMovies";
  }

  @Override public int getPriority() {
    return PRIORITY_2;
  }

  @Override public void perform() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());

    final String lastUpdated = settings.getString(Settings.MOVIES_LAST_UPDATED, null);
    String currentTime = TimeUtils.getIsoTime();

    if (lastUpdated != null) {
      long millis = TimeUtils.getMillis(lastUpdated);
      millis = millis - 12 * DateUtils.HOUR_IN_MILLIS;
      String updatedSince = TimeUtils.getIsoTime(millis);
      Timber.d("Last updated: " + lastUpdated + " - updated since: " + updatedSince);
      queue(new SyncUpdatedMovies(updatedSince, 1));
    }

    settings.edit().putString(Settings.MOVIES_LAST_UPDATED, currentTime).apply();
  }
}
