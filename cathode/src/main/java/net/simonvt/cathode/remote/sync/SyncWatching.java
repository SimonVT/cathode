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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.Watching;
import net.simonvt.cathode.api.enumeration.Action;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.shows.SyncSeason;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import retrofit.Call;
import timber.log.Timber;

public class SyncWatching extends CallJob<Watching> {

  @Inject transient UsersService usersService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;

  public SyncWatching() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncWatching";
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<Watching> getCall() {
    return usersService.watching();
  }

  @Override public void handleResponse(Watching watching) {
    ContentResolver resolver = getContentResolver();

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    Cursor episodeWatchingCursor =
        getContentResolver().query(Episodes.EPISODE_WATCHING, new String[] {
            Tables.EPISODES + "." + EpisodeColumns.ID,
        }, null, null, null);

    List<Long> episodeWatching = new ArrayList<Long>();
    while (episodeWatchingCursor.moveToNext()) {
      episodeWatching.add(
          episodeWatchingCursor.getLong(episodeWatchingCursor.getColumnIndex(EpisodeColumns.ID)));
    }
    episodeWatchingCursor.close();

    Cursor movieWatchingCursor = getContentResolver().query(Movies.WATCHING, new String[] {
        MovieColumns.ID,
    }, null, null, null);

    List<Long> movieWatching = new ArrayList<Long>();
    while (movieWatchingCursor.moveToNext()) {
      movieWatching.add(
          movieWatchingCursor.getLong(movieWatchingCursor.getColumnIndex(MovieColumns.ID)));
    }
    movieWatchingCursor.close();

    ContentProviderOperation op = null;

    if (watching != null) {
      if (watching.getType() != null) {
        switch (watching.getType()) {
          case EPISODE:
            final long showTraktId = watching.getShow().getIds().getTrakt();

            ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(showTraktId);
            final long showId = showResult.showId;
            final boolean didShowExist = !showResult.didCreate;
            if (showResult.didCreate) {
              queue(new SyncShow(showTraktId));
            }

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
                queue(new SyncSeason(showTraktId, seasonNumber));
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

          case MOVIE:
            final long movieTraktId = watching.getMovie().getIds().getTrakt();
            long movieId = MovieWrapper.getMovieId(getContentResolver(), movieTraktId);
            if (movieId == -1L) {
              movieId = MovieWrapper.createMovie(getContentResolver(), movieTraktId);
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

    for (Long episodeId : episodeWatching) {
      op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
          .withValue(EpisodeColumns.CHECKED_IN, false)
          .withValue(EpisodeColumns.WATCHING, false)
          .build();
      ops.add(op);
    }

    for (Long movieId : movieWatching) {
      op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
          .withValue(MovieColumns.CHECKED_IN, false)
          .withValue(MovieColumns.WATCHING, false)
          .build();
      ops.add(op);
    }

    try {
      resolver.applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "SyncWatchingTask failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncWatchingTask failed");
      throw new JobFailedException(e);
    }
  }
}
