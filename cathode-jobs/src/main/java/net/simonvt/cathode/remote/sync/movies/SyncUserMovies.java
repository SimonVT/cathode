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

package net.simonvt.cathode.remote.sync.movies;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;
import java.util.ArrayList;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.jobscheduler.SchedulerService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public class SyncUserMovies extends Job {

  public static final int ID = 107;

  private static final long SYNC_INTERVAL = 30 * DateUtils.DAY_IN_MILLIS;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public static void schedulePeriodic(Context context) {
    JobInfo jobInfo = new JobInfo.Builder(ID, new ComponentName(context, SchedulerService.class)) //
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setRequiresCharging(true)
        .setRequiresDeviceIdle(true)
        .setPeriodic(DateUtils.DAY_IN_MILLIS)
        .setPersisted(true)
        .build();
    Jobs.scheduleNotPending(context, jobInfo);
  }

  @Override public String key() {
    return "SyncUserMovies";
  }

  @Override public int getPriority() {
    return JobPriority.UPDATED;
  }

  @Override public boolean perform() {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    final long syncBefore = System.currentTimeMillis() - SYNC_INTERVAL;
    Cursor movies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, "("
        + MovieColumns.WATCHED
        + " OR "
        + MovieColumns.IN_COLLECTION
        + " OR "
        + MovieColumns.IN_WATCHLIST
        + ") AND "
        + MovieColumns.LAST_SYNC
        + "<?", new String[] {
        String.valueOf(syncBefore),
    }, null);
    while (movies.moveToNext()) {
      final long movieId = Cursors.getLong(movies, MovieColumns.ID);
      Timber.d("Found one! %d", movieId);
      ContentValues values = new ContentValues();
      values.put(MovieColumns.NEEDS_SYNC, true);
      ops.add(
          ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(values).build());
    }
    movies.close();

    if (Jobs.usesScheduler()) {
      SyncPendingMovies.schedule(getContext());
    } else {
      queue(new SyncPendingMovies());
    }

    if (!applyBatch(ops)) {
      return false;
    }

    return true;
  }
}
