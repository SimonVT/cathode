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
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.ServerTime;
import net.simonvt.cathode.api.entity.UpdatedShows;
import net.simonvt.cathode.api.service.ServerService;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.Settings;

public class SyncUpdatedShows extends TraktTask {

  @Inject transient ShowsService showsService;

  @Inject transient ServerService serverService;

  @Override protected void doTask() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(service);
    final long showsLastUpdated = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0);

    long current;

    if (showsLastUpdated > 0) {
      UpdatedShows updatedShows = showsService.updated(showsLastUpdated);
      current = updatedShows.getTimestamps().getCurrent();

      List<UpdatedShows.ShowTimestamp> timestamps = updatedShows.getShows();
      for (UpdatedShows.ShowTimestamp timestamp : timestamps) {
        final int tvdbId = timestamp.getTvdbId();
        final boolean exists = ShowWrapper.exists(service.getContentResolver(), tvdbId);
        if (exists) {
          final boolean needsUpdate = ShowWrapper.needsUpdate(service.getContentResolver(), tvdbId,
              timestamp.getLastUpdated());
          if (needsUpdate) {
            queueTask(new SyncShowTask(tvdbId));
          }
        }
      }
    } else {
      queueTask(new SyncShowsTask());
      ServerTime time = serverService.time();
      current = time.getTimestamp();
    }

    settings.edit().putLong(Settings.SHOWS_LAST_UPDATED, current).apply();

    postOnSuccess();
  }
}
