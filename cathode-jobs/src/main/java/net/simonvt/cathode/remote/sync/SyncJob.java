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
package net.simonvt.cathode.remote.sync;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.simonvt.cathode.common.util.DateUtils;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.tmdb.api.SyncConfiguration;

public class SyncJob extends Job {

  public SyncJob() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncJob";
  }

  @Override public int getPriority() {
    return JobPriority.USER_DATA;
  }

  @Override public boolean perform() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    final long currentTime = System.currentTimeMillis();
    final long lastConfigSync = settings.getLong(Settings.LAST_CONFIG_SYNC, 0L);
    final long lastShowSync = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0L);
    final long lastMovieSync = settings.getLong(Settings.MOVIES_LAST_UPDATED, 0L);

    if (lastConfigSync + DateUtils.DAY_IN_MILLIS < currentTime) {
      queue(new SyncUserSettings());
      queue(new SyncConfiguration());
      settings.edit().putLong(Settings.LAST_CONFIG_SYNC, currentTime).apply();
    }
    if (lastShowSync == 0L) {
      settings.edit().putLong(Settings.SHOWS_LAST_UPDATED, currentTime).apply();
    } else if (lastShowSync + getUpdatedSyncDelay() * DateUtils.DAY_IN_MILLIS < currentTime) {
      queue(new SyncUpdatedShows());
    }
    if (lastMovieSync == 0L) {
      settings.edit().putLong(Settings.MOVIES_LAST_UPDATED, currentTime).apply();
    } else if (lastMovieSync + getUpdatedSyncDelay() * DateUtils.DAY_IN_MILLIS < currentTime) {
      queue(new SyncUpdatedMovies());
    }

    queue(new SyncUserActivity());
    queue(new SyncWatching());

    settings.edit().putLong(Settings.LAST_FULL_SYNC, currentTime).apply();
    return true;
  }

  private long getUpdatedSyncDelay() {
    if (Jobs.usesScheduler()) {
      return 7 * DateUtils.DAY_IN_MILLIS;
    } else {
      return 2 * DateUtils.DAY_IN_MILLIS;
    }
  }
}
