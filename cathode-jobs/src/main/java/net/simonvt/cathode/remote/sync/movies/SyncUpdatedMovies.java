/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.UpdatedItem;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.jobscheduler.SchedulerService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.SeparatePagesCallJob;
import net.simonvt.cathode.settings.Settings;
import retrofit2.Call;

public class SyncUpdatedMovies extends SeparatePagesCallJob<UpdatedItem> {

  public static final int ID = 101;

  private static final int LIMIT = 100;

  @Inject transient MoviesService moviesService;
  @Inject transient MovieDatabaseHelper movieHelper;

  private transient SharedPreferences settings;
  private transient long currentTime;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public static void schedulePeriodic(Context context) {
    JobInfo jobInfo = new JobInfo.Builder(ID, new ComponentName(context, SchedulerService.class)) //
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setRequiresCharging(true)
        .setRequiresDeviceIdle(true)
        .setPeriodic(android.text.format.DateUtils.DAY_IN_MILLIS)
        .setPersisted(true)
        .build();
    Jobs.scheduleNotPending(context, jobInfo);
  }

  public SyncUpdatedMovies() {
    currentTime = System.currentTimeMillis();
  }

  @Override public String key() {
    return "SyncUpdatedMovies";
  }

  @Override public int getPriority() {
    return JobPriority.UPDATED;
  }

  @Override public Call<List<UpdatedItem>> getCall(int page) {
    settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    final long lastUpdated = settings.getLong(Settings.MOVIES_LAST_UPDATED, currentTime);
    final long millis = lastUpdated - 12 * DateUtils.HOUR_IN_MILLIS;
    final String updatedSince = TimeUtils.getIsoTime(millis);
    return moviesService.updated(updatedSince, page, LIMIT);
  }

  @Override public boolean handleResponse(int page, List<UpdatedItem> updated) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (UpdatedItem item : updated) {
      final long updatedAt = item.getUpdatedAt().getTimeInMillis();
      final Movie movie = item.getMovie();
      final long traktId = movie.getIds().getTrakt();

      final long movieId = movieHelper.getId(traktId);
      if (movieId != -1L) {
        if (movieHelper.isUpdated(traktId, updatedAt)) {
          ContentValues values = new ContentValues();
          values.put(MovieColumns.NEEDS_SYNC, true);
          ops.add(ContentProviderOperation.newUpdate(Movies.withId(movieId))
              .withValues(values)
              .build());
        }
      }
    }

    if (!applyBatch(ops)) {
      return false;
    }

    return true;
  }

  @Override public boolean onDone() {
    if (Jobs.usesScheduler()) {
      SyncPendingMovies.schedule(getContext());
    } else {
      queue(new SyncPendingMovies());
    }

    settings.edit().putLong(Settings.MOVIES_LAST_UPDATED, currentTime).apply();
    return true;
  }
}
