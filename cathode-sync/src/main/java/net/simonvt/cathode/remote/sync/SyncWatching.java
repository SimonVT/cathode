/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Watching;
import net.simonvt.cathode.api.enumeration.Action;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.movies.SyncWatchedMovies;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.cathode.remote.sync.shows.SyncShowWatchedStatus;
import net.simonvt.cathode.sync.jobscheduler.Jobs;
import net.simonvt.cathode.sync.jobscheduler.SchedulerService;
import retrofit2.Call;

public class SyncWatching extends CallJob<Watching> {

  public static final int ID = 200;

  @Inject transient UsersService usersService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;
  @Inject transient MovieDatabaseHelper movieHelper;

  public static void schedule(Context context, long delayMillis) {
    JobInfo jobInfo = new JobInfo.Builder(ID, new ComponentName(context, SchedulerService.class)) //
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setBackoffCriteria(DateUtils.MINUTE_IN_MILLIS, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
        .setMinimumLatency(delayMillis)
        .setPersisted(true)
        .build();
    Jobs.schedule(context, jobInfo);
  }

  public SyncWatching() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncWatching";
  }

  @Override public int getPriority() {
    return JobPriority.ACTIONS;
  }

  @Override public Call<Watching> getCall() {
    return usersService.watching();
  }

  @Override public boolean handleResponse(Watching watching) {
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    Cursor episodeWatchingCursor =
        getContentResolver().query(Episodes.EPISODE_WATCHING, new String[] {
            Tables.EPISODES + "." + EpisodeColumns.ID,
        }, null, null, null);

    List<Long> episodeWatching = new ArrayList<>();
    while (episodeWatchingCursor.moveToNext()) {
      episodeWatching.add(Cursors.getLong(episodeWatchingCursor, EpisodeColumns.ID));
    }
    episodeWatchingCursor.close();

    Cursor movieWatchingCursor = getContentResolver().query(Movies.WATCHING, new String[] {
        MovieColumns.ID,
    }, null, null, null);

    List<Long> movieWatching = new ArrayList<>();
    while (movieWatchingCursor.moveToNext()) {
      movieWatching.add(Cursors.getLong(movieWatchingCursor, MovieColumns.ID));
    }
    movieWatchingCursor.close();

    ContentProviderOperation op;

    if (watching != null) {
      if (watching.getType() != null) {
        switch (watching.getType()) {
          case EPISODE: {
            final long showTraktId = watching.getShow().getIds().getTrakt();

            ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(showTraktId);
            final long showId = showResult.showId;

            if (showHelper.needsSync(showId)) {
              queue(new SyncShow(showTraktId));
            }

            final boolean didShowExist = !showResult.didCreate;

            final int seasonNumber = watching.getEpisode().getSeason();
            SeasonDatabaseHelper.IdResult seasonResult =
                seasonHelper.getIdOrCreate(showId, seasonNumber);
            final long seasonId = seasonResult.id;
            final boolean didSeasonExist = !seasonResult.didCreate;
            if (seasonResult.didCreate) {
              if (didShowExist) {
                queue(new SyncShow(showTraktId));
              }
            }

            final int episodeNumber = watching.getEpisode().getNumber();

            EpisodeDatabaseHelper.IdResult episodeResult =
                episodeHelper.getIdOrCreate(showId, seasonId, episodeNumber);
            final long episodeId = episodeResult.id;
            if (episodeResult.didCreate) {
              if (didShowExist && didSeasonExist) {
                queue(new SyncShow(showTraktId));
              }
            }

            episodeWatching.remove(episodeId);

            if (watching.getAction() == Action.CHECKIN) {
              op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
                  .withValue(EpisodeColumns.CHECKED_IN, true)
                  .withValue(EpisodeColumns.WATCHING, false)
                  .withValue(EpisodeColumns.STARTED_AT, watching.getStartedAt().getTimeInMillis())
                  .withValue(EpisodeColumns.EXPIRES_AT, watching.getExpiresAt().getTimeInMillis())
                  .build();
            } else {
              op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
                  .withValue(EpisodeColumns.CHECKED_IN, false)
                  .withValue(EpisodeColumns.WATCHING, true)
                  .withValue(EpisodeColumns.STARTED_AT, watching.getStartedAt().getTimeInMillis())
                  .withValue(EpisodeColumns.EXPIRES_AT, watching.getExpiresAt().getTimeInMillis())
                  .build();
            }
            ops.add(op);
            break;
          }

          case MOVIE: {
            final long movieTraktId = watching.getMovie().getIds().getTrakt();
            MovieDatabaseHelper.IdResult result = movieHelper.getIdOrCreate(movieTraktId);
            final long movieId = result.movieId;

            if (movieHelper.needsSync(movieId)) {
              queue(new SyncMovie(movieTraktId));
            }

            movieWatching.remove(movieId);

            if (watching.getAction() == Action.CHECKIN) {
              op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
                  .withValue(MovieColumns.CHECKED_IN, true)
                  .withValue(MovieColumns.WATCHING, false)
                  .withValue(MovieColumns.STARTED_AT, watching.getStartedAt().getTimeInMillis())
                  .withValue(MovieColumns.EXPIRES_AT, watching.getExpiresAt().getTimeInMillis())
                  .build();
            } else {
              op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
                  .withValue(MovieColumns.CHECKED_IN, false)
                  .withValue(MovieColumns.WATCHING, true)
                  .withValue(MovieColumns.STARTED_AT, watching.getStartedAt().getTimeInMillis())
                  .withValue(MovieColumns.EXPIRES_AT, watching.getExpiresAt().getTimeInMillis())
                  .build();
            }
            ops.add(op);
            break;
          }
        }
      }
    }

    for (Long episodeId : episodeWatching) {
      final long showId = episodeHelper.getShowId(episodeId);
      final long showTraktId = showHelper.getTraktId(showId);
      queue(new SyncShowWatchedStatus(showTraktId));

      op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
          .withValue(EpisodeColumns.CHECKED_IN, false)
          .withValue(EpisodeColumns.WATCHING, false)
          .build();
      ops.add(op);
    }

    for (Long movieId : movieWatching) {
      queue(new SyncWatchedMovies());

      op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
          .withValue(MovieColumns.CHECKED_IN, false)
          .withValue(MovieColumns.WATCHING, false)
          .build();
      ops.add(op);
    }

    return applyBatch(ops);
  }
}
