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
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.remote.sync.movies.SyncMovieRecommendations;
import net.simonvt.cathode.remote.sync.movies.SyncTrendingMoviesTask;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.shows.SyncShowRecommendations;
import net.simonvt.cathode.remote.sync.shows.SyncTrendingShowsTask;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktTimestamps;

public class SyncTask extends TraktTask {

  @Override protected void doTask() {
    if (TraktTimestamps.shouldPurge(getContext())) {
      queueTask(new PurgeTask());
    }

    queueTask(new SyncUserSettingsTask());
    queueTask(new SyncActivityStreamTask());

    queueTask(new SyncUpdatedShows());
    queueTask(new SyncUpdatedMovies());

    /**
     * The trending tasks adds shows and movies but doesn't do a complete sync of them. Because they
     * then exist, the tasks in SyncUserActivity will sync the individual episodes instead of the
     * entire show/movie. By allowing these tasks to execute before the trending shows tasks,
     * a full sync is scheduled. This only really matters for the first sync.
     */
    new SyncUserActivityTask().execute(context, null);

    if (TraktTimestamps.trendingNeedsUpdate(getContext())) {
      TraktTimestamps.updateTrending(getContext());
      queueTask(new SyncTrendingShowsTask());
      queueTask(new SyncTrendingMoviesTask());
    }

    if (TraktTimestamps.recommendationsNeedsUpdate(getContext())) {
      TraktTimestamps.updateRecommendations(getContext());
      queueTask(new SyncShowRecommendations());
      queueTask(new SyncMovieRecommendations());
    }

    final long currentTime = System.currentTimeMillis();
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    settings.edit().putLong(Settings.FULL_SYNC, currentTime).apply();

    postOnSuccess();
  }
}
