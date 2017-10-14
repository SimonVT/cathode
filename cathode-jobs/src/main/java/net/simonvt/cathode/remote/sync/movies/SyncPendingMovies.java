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
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.common.event.ItemsUpdatedEvent;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.jobscheduler.SchedulerService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.remote.ErrorHandlerJob;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class SyncPendingMovies extends ErrorHandlerJob<Movie> {

  public static final int ID = 105;

  @Inject transient MoviesService moviesService;
  @Inject transient MovieDatabaseHelper movieHelper;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) public static void schedule(Context context) {
    JobInfo jobInfo = new JobInfo.Builder(ID, new ComponentName(context, SchedulerService.class)) //
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setRequiresCharging(true)
        .setBackoffCriteria(DateUtils.MINUTE_IN_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
        .setPersisted(true)
        .build();
    Jobs.schedule(context, jobInfo);
  }

  @Override public String key() {
    return "SyncPendingMovies";
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return JobPriority.UPDATED;
  }

  @Override public boolean perform() {
    final String where = MovieColumns.NEEDS_SYNC
        + "=1 AND ("
        + MovieColumns.WATCHED
        + "=1 OR "
        + MovieColumns.IN_COLLECTION
        + "=1 OR "
        + MovieColumns.IN_WATCHLIST
        + "=1)";
    Cursor movies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.TRAKT_ID,
    }, where, null, null);
    try {
      while (movies.moveToNext() && !isStopped()) {
        final long traktId = Cursors.getLong(movies, MovieColumns.TRAKT_ID);
        Timber.d("Syncing pending movie %d", traktId);

        Call<Movie> call = moviesService.getSummary(traktId, Extended.FULL);
        Response<Movie> response = call.execute();
        if (response.isSuccessful()) {
          Movie movie = response.body();
          movieHelper.fullUpdate(movie);
        } else {
          if (isError(response)) {
            return false;
          }
        }
      }

      ItemsUpdatedEvent.post();

      if (isStopped()) {
        return false;
      }

      return true;
    } catch (IOException e) {
      Timber.d(e);
      return false;
    } finally {
      if (movies != null) {
        movies.close();
      }
    }
  }
}
