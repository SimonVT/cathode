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
import net.simonvt.cathode.api.entity.UpdatedMovies;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.Settings;
import retrofit.RetrofitError;

public class SyncUpdatedMovies extends TraktTask {

  private static final String TAG = "SyncUpdatedMovies";

  @Inject transient MoviesService moviesService;

  @Override protected void doTask() {
    try {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(service);
      final long moviesLastUpdated = settings.getLong(Settings.MOVIES_LAST_UPDATED, 0);

      UpdatedMovies updatedMovies = moviesService.updated(moviesLastUpdated);

      List<UpdatedMovies.MovieTimestamp> timestamps = updatedMovies.getMovies();
      for (UpdatedMovies.MovieTimestamp timestamp : timestamps) {
        final int tmdbId = timestamp.getTmdbId();
        final boolean exists = MovieWrapper.exists(service.getContentResolver(), tmdbId);
        if (exists) {
          final boolean needsUpdate =
              MovieWrapper.needsUpdate(service.getContentResolver(), tmdbId,
                  timestamp.getLastUpdated());
          if (needsUpdate) {
            queueTask(new SyncMovieTask(tmdbId));
          }
        }
      }

      settings.edit()
          .putLong(Settings.MOVIES_LAST_UPDATED, updatedMovies.getTimestamps().getCurrent())
          .apply();

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}
