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

package net.simonvt.cathode.remote.sync.shows;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.SeasonService;
import net.simonvt.cathode.common.event.ItemsUpdatedEvent;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.jobscheduler.SchedulerService;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.ErrorHandlerJob;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class SyncPendingSeasons extends ErrorHandlerJob<List<Episode>> {

  public static final int ID = 104;

  @Inject transient SeasonService seasonService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

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
    return "SyncPendingSeasons";
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return JobPriority.SEASONS;
  }

  @Override public boolean perform() {
    Cursor seasons = getContentResolver().query(Seasons.SEASONS, new String[] {
        SeasonColumns.ID, SeasonColumns.SHOW_ID, SeasonColumns.SEASON,
    }, SeasonColumns.NEEDS_SYNC, null, null);

    try {
      while (seasons.moveToNext() && !isStopped()) {
        final long seasonId = Cursors.getLong(seasons, SeasonColumns.ID);
        final long showId = Cursors.getLong(seasons, SeasonColumns.SHOW_ID);
        final int season = Cursors.getInt(seasons, SeasonColumns.SEASON);

        final long traktId = showHelper.getTraktId(showId);

        Timber.d("Syncing pending season %d-%d", traktId, season);

        Call<List<Episode>> call = seasonService.getSeason(traktId, season, Extended.FULL);
        Response<List<Episode>> response = call.execute();
        if (response.isSuccessful()) {
          List<Episode> episodes = response.body();

          Cursor c = getContentResolver().query(Episodes.fromSeason(seasonId), new String[] {
              EpisodeColumns.ID
          }, null, null, null);
          List<Long> episodeIds = new ArrayList<>();
          while (c.moveToNext()) {
            final long episodeId = Cursors.getLong(c, EpisodeColumns.ID);
            episodeIds.add(episodeId);
          }
          c.close();

          for (Episode episode : episodes) {
            EpisodeDatabaseHelper.IdResult episodeResult =
                episodeHelper.getIdOrCreate(showId, seasonId, episode.getNumber());
            final long episodeId = episodeResult.id;
            episodeHelper.updateEpisode(episodeId, episode);
            episodeIds.remove(episodeId);
          }

          for (Long episodeId : episodeIds) {
            getContentResolver().delete(Episodes.withId(episodeId), null, null);
          }

          ContentValues values = new ContentValues();
          values.put(SeasonColumns.NEEDS_SYNC, false);
          getContentResolver().update(Seasons.withId(seasonId), values, null, null);
        } else {
          boolean error = isError(response);
          if (error) {
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
      if (seasons != null) {
        seasons.close();
      }
    }
  }
}
