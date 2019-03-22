/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.remote;

import android.content.ContentValues;
import androidx.work.WorkManager;
import javax.inject.Inject;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.work.WorkManagerUtils;
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker;
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker;

public class ForceUpdateJob extends Job {

  @Inject transient WorkManager workManager;
  @Inject transient ShowDatabaseHelper showHelper;

  @Override public String key() {
    return "ForceUpdateJob";
  }

  @Override public int getPriority() {
    return JobPriority.ACTIONS;
  }

  @Override public boolean perform() {
    ContentValues values = new ContentValues();
    values.put(ShowColumns.NEEDS_SYNC, true);
    getContentResolver().update(Shows.SHOWS, values, null, null);

    values.clear();
    values.put(MovieColumns.NEEDS_SYNC, true);
    getContentResolver().update(Movies.MOVIES, values, null, null);

    WorkManagerUtils.enqueueUniqueNow(workManager, SyncPendingShowsWorker.TAG, SyncPendingShowsWorker.class);
    WorkManagerUtils.enqueueUniqueNow(workManager, SyncPendingMoviesWorker.TAG, SyncPendingMoviesWorker.class);

    return true;
  }
}
