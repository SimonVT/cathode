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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.entity.Watching;
import net.simonvt.cathode.api.enumeration.Action;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.remote.sync.movies.SyncMovieTask;
import net.simonvt.cathode.remote.sync.shows.SyncEpisodeTask;
import net.simonvt.cathode.remote.sync.shows.SyncShowCast;
import net.simonvt.cathode.remote.sync.shows.SyncShowTask;
import net.simonvt.cathode.settings.Settings;
import timber.log.Timber;

public class SyncWatchingTask extends TraktTask {

  @Inject transient UsersService usersService;

  @Override protected void doTask() {
    // TODO: Tell the difference between scrobbles and checkins
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

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
    String username = settings.getString(Settings.PROFILE_USERNAME, null);

    if (TextUtils.isEmpty(username)) {
      queueTask(new SyncUserSettingsTask());
      queueTask(new SyncWatchingTask());
      postOnSuccess();
      return;
    }

    Watching watching = usersService.watching(username);

    if (watching != null) {
      if (watching.getType() != null) {
        switch (watching.getType()) {
          case EPISODE:
            final long showTraktId = watching.getShow().getIds().getTrakt();

            boolean didShowExist = true;
            long showId = ShowWrapper.getShowId(getContentResolver(), showTraktId);
            if (showId == -1L) {
              didShowExist = false;
              showId = ShowWrapper.createShow(getContentResolver(), showTraktId);
              queueTask(new SyncShowCast(showTraktId));
            }

            final int seasonNumber = watching.getEpisode().getSeason();
            boolean didSeasonExist = true;
            long seasonId = SeasonWrapper.getSeasonId(getContentResolver(), showId, seasonNumber);
            if (seasonId == -1L) {
              didSeasonExist = false;
              if (didShowExist) {
                queueTask(new SyncShowTask(showTraktId));
              }
            }

            final int episodeNumber = watching.getEpisode().getNumber();
            long episodeId = EpisodeWrapper.getEpisodeId(getContentResolver(), showId, seasonNumber,
                episodeNumber);
            if (episodeId == -1L) {
              episodeId = EpisodeWrapper.createEpisode(getContentResolver(), showId, seasonId,
                  episodeNumber);
              if (didSeasonExist) {
                queueTask(new SyncEpisodeTask(showTraktId, seasonNumber, episodeNumber));
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
              queueTask(new SyncMovieTask(movieTraktId));
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
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncWatchingTask failed");
    }

    postOnSuccess();
  }
}
