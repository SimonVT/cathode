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
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.movies.SyncMovieRecommendations;
import net.simonvt.cathode.remote.sync.movies.SyncTrendingMovies;
import net.simonvt.cathode.remote.sync.movies.StartSyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.shows.SyncShowRecommendations;
import net.simonvt.cathode.remote.sync.shows.SyncTrendingShows;
import net.simonvt.cathode.remote.sync.shows.StartSyncUpdatedShows;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.jobqueue.Job;

public class SyncJob extends Job {

  public SyncJob() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncJob";
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  @Override public void perform() {
    if (TraktTimestamps.shouldPurge(getContext())) {
      queue(new PurgeDatabase());
    }

    queue(new SyncUserSettings());
    queue(new SyncActivityStream());

    queue(new StartSyncUpdatedShows());
    queue(new StartSyncUpdatedMovies());

    queue(new SyncUserActivity());

    if (TraktTimestamps.trendingNeedsUpdate(getContext())) {
      TraktTimestamps.updateTrending(getContext());
      queue(new SyncTrendingShows());
      queue(new SyncTrendingMovies());
    }

    if (TraktTimestamps.recommendationsNeedsUpdate(getContext())) {
      TraktTimestamps.updateRecommendations(getContext());
      queue(new SyncShowRecommendations());
      queue(new SyncMovieRecommendations());
    }

    final long currentTime = System.currentTimeMillis();
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    settings.edit().putLong(Settings.FULL_SYNC, currentTime).apply();
  }
}
