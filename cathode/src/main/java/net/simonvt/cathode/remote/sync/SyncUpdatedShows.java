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
import java.util.ArrayList;
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
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    final long lastUpdated = settings.getLong(Settings.SHOWS_LAST_UPDATED, 0);

    List<Integer> showSummaries = new ArrayList<Integer>();

    long currentTime;

    if (lastUpdated == 0) {
      ServerTime time = serverService.time();
      currentTime = time.getTimestamp();
    } else {
      UpdatedShows updatedShows = showsService.updated(lastUpdated);
      currentTime = updatedShows.getTimestamps().getCurrent();

      List<UpdatedShows.ShowTimestamp> timestamps = updatedShows.getShows();
      for (UpdatedShows.ShowTimestamp timestamp : timestamps) {
        final int tvdbId = timestamp.getTvdbId();
        final boolean exists = ShowWrapper.exists(getContentResolver(), tvdbId);
        if (exists) {
          final boolean needsUpdate =
              ShowWrapper.needsUpdate(getContentResolver(), tvdbId, timestamp.getLastUpdated());
          if (needsUpdate) {
            final long id = ShowWrapper.getShowId(getContentResolver(), tvdbId);
            if (ShowWrapper.shouldSyncFully(getContentResolver(), id)) {
              queueTask(new SyncShowTask(tvdbId));
            } else {
              showSummaries.add(tvdbId);
            }
          }
        }
      }

      if (showSummaries.size() > 0) {
        queueTask(new SyncShowSummariesTask(showSummaries));
      }
    }

    settings.edit().putLong(Settings.SHOWS_LAST_UPDATED, currentTime).apply();
    postOnSuccess();
  }
}
