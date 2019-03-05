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

import android.text.format.DateUtils;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncUserMovies;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncUserShows;
import net.simonvt.cathode.settings.Timestamps;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.sync.tmdb.api.SyncConfiguration;

public class SyncJob extends Job {

  private static final long UPDATE_SYNC_DELAY = 7 * DateUtils.DAY_IN_MILLIS;

  @Override public String key() {
    return "SyncJob";
  }

  @Override public int getPriority() {
    return JobPriority.USER_DATA;
  }

  @Override public boolean perform() {
    final long currentTime = System.currentTimeMillis();
    final long lastConfigSync =
        Timestamps.get(getContext()).getLong(Timestamps.LAST_CONFIG_SYNC, 0L);
    final long lastShowSync =
        Timestamps.get(getContext()).getLong(Timestamps.SHOWS_LAST_UPDATED, 0L);
    final long lastMovieSync =
        Timestamps.get(getContext()).getLong(Timestamps.MOVIES_LAST_UPDATED, 0L);

    if (lastConfigSync + DateUtils.DAY_IN_MILLIS < currentTime) {
      if (TraktLinkSettings.isLinked(getContext())) {
        queue(new SyncUserSettings());
      }

      queue(new SyncConfiguration());
      Timestamps.get(getContext()).edit().putLong(Timestamps.LAST_CONFIG_SYNC, currentTime).apply();
    }
    if (lastShowSync == 0L) {
      Timestamps.get(getContext())
          .edit()
          .putLong(Timestamps.SHOWS_LAST_UPDATED, currentTime)
          .apply();
    } else if (lastShowSync + UPDATE_SYNC_DELAY < currentTime) {
      queue(new SyncUpdatedShows());
    }
    if (lastMovieSync == 0L) {
      Timestamps.get(getContext())
          .edit()
          .putLong(Timestamps.MOVIES_LAST_UPDATED, currentTime)
          .apply();
    } else if (lastMovieSync + UPDATE_SYNC_DELAY < currentTime) {
      queue(new SyncUpdatedMovies());
    }

    queue(new SyncUserShows());
    queue(new SyncUserMovies());

    if (TraktLinkSettings.isLinked(getContext())) {
      queue(new SyncUserActivity());
      queue(new SyncWatching());
    }

    Timestamps.get(getContext()).edit().putLong(Timestamps.LAST_FULL_SYNC, currentTime).apply();
    return true;
  }
}
