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
import net.simonvt.cathode.api.entity.UpdatedMovies;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.api.service.ServerService;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.Settings;

public class SyncUpdatedMovies extends TraktTask {

  @Inject transient MoviesService moviesService;
  @Inject transient ServerService serverService;

  @Override protected void doTask() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    final long lastUpdated = settings.getLong(Settings.MOVIES_LAST_UPDATED, 0);
    long currentTime;

    if (lastUpdated == 0) {
      ServerTime time = serverService.time();
      currentTime = time.getTimestamp();
    } else {
      UpdatedMovies updatedMovies = moviesService.updated(lastUpdated);

      List<UpdatedMovies.MovieTimestamp> timestamps = updatedMovies.getMovies();
      for (UpdatedMovies.MovieTimestamp timestamp : timestamps) {
        final int tmdbId = timestamp.getTmdbId();
        final boolean exists = MovieWrapper.exists(getContentResolver(), tmdbId);
        if (exists) {
          final boolean needsUpdate =
              MovieWrapper.needsUpdate(getContentResolver(), tmdbId, timestamp.getLastUpdated());
          if (needsUpdate) {
            queueTask(new SyncMovieTask(tmdbId));
          }
        }
      }

      currentTime = updatedMovies.getTimestamps().getCurrent();
    }

    settings.edit().putLong(Settings.MOVIES_LAST_UPDATED, currentTime).apply();

    postOnSuccess();
  }
}
