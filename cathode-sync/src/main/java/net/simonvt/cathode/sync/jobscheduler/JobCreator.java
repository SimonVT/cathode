/*
 * Copyright (C) 2017 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use syncService file except in compliance with the License.
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

package net.simonvt.cathode.sync.jobscheduler;

import android.app.job.JobParameters;
import dagger.android.DispatchingAndroidInjector;
import javax.inject.Inject;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.movies.SyncPendingMovies;
import net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.movies.SyncUserMovies;
import net.simonvt.cathode.remote.sync.shows.SyncPendingSeasons;
import net.simonvt.cathode.remote.sync.shows.SyncPendingShows;
import net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows;
import net.simonvt.cathode.remote.sync.shows.SyncUserShows;

public class JobCreator {

  @Inject DispatchingAndroidInjector<Job> jobInjector;

  @Inject public JobCreator(DispatchingAndroidInjector<Job> jobInjector) {
    this.jobInjector = jobInjector;
  }

  public Job create(JobParameters params) {
    switch (params.getJobId()) {
      case SyncUpdatedShows.ID:
        return inject(new SyncUpdatedShows());

      case SyncUserShows.ID:
        return inject(new SyncUserShows());

      case SyncUpdatedMovies.ID:
        return inject(new SyncUpdatedMovies());

      case SyncUserMovies.ID:
        return inject(new SyncUserMovies());

      case SyncUserActivity.ID:
        return inject(new SyncUserActivity());

      case SyncPendingShows.ID:
        return inject(new SyncPendingShows());

      case SyncPendingSeasons.ID:
        return inject(new SyncPendingSeasons());

      case SyncPendingMovies.ID:
        return inject(new SyncPendingMovies());

      case SyncWatching.ID:
        return inject(new SyncWatching());

      default:
        throw new IllegalArgumentException("Unknown job id " + params.getJobId());
    }
  }

  private Job inject(Job job) {
    jobInjector.inject(job);
    return job;
  }
}
