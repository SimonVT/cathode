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
package net.simonvt.cathode.remote.sync.shows;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.entity.UpdatedItem;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.SeparatePagesCallJob;
import net.simonvt.cathode.settings.Timestamps;
import net.simonvt.cathode.sync.jobscheduler.Jobs;
import net.simonvt.cathode.sync.jobscheduler.SchedulerService;
import retrofit2.Call;

public class SyncUpdatedShows extends SeparatePagesCallJob<UpdatedItem> {

  public static final int ID = 100;

  private static final int LIMIT = 100;

  @Inject transient ShowsService showsService;
  @Inject transient ShowDatabaseHelper showHelper;

  private transient long currentTime;

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

  public SyncUpdatedShows() {
    currentTime = System.currentTimeMillis();
  }

  @Override public String key() {
    return "SyncUpdatedShows";
  }

  @Override public int getPriority() {
    return JobPriority.UPDATED;
  }

  @Override public Call<List<UpdatedItem>> getCall(int page) {
    final long lastUpdated =
        Timestamps.get(getContext()).getLong(Timestamps.SHOWS_LAST_UPDATED, currentTime);
    final long millis = lastUpdated - 12 * DateUtils.HOUR_IN_MILLIS;
    final String updatedSince = TimeUtils.getIsoTime(millis);
    return showsService.getUpdatedShows(updatedSince, page, LIMIT);
  }

  @Override public boolean handleResponse(int page, List<UpdatedItem> updated) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (UpdatedItem item : updated) {
      final long updatedAt = item.getUpdatedAt().getTimeInMillis();

      Show show = item.getShow();
      final long traktId = show.getIds().getTrakt();
      final long id = showHelper.getId(traktId);
      if (id != -1L) {
        if (showHelper.isUpdated(traktId, updatedAt)) {
          ContentValues values = new ContentValues();
          values.put(ShowColumns.NEEDS_SYNC, true);
          ops.add(ContentProviderOperation.newUpdate(Shows.withId(id)).withValues(values).build());
        }
      }
    }

    if (!applyBatch(ops)) {
      return false;
    }

    return true;
  }

  @Override public boolean onDone() {
    SyncPendingShows.schedule(getContext());
    Timestamps.get(getContext()).edit().putLong(Timestamps.SHOWS_LAST_UPDATED, currentTime).apply();
    return true;
  }
}
