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
import net.simonvt.cathode.api.entity.ActivityItem;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.enumeration.ActivityAction;
import net.simonvt.cathode.api.enumeration.ActivityType;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncWatchingTask extends TraktTask {

  @Inject transient UserService userService;

  @Override protected void doTask() {
    ContentResolver resolver = getContentResolver();

    ActivityItem activity = userService.watching();

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

    if (activity != null) {
      ActivityType type = activity.getType();
      ActivityAction action = activity.getAction();

      if (type == ActivityType.EPISODE) {
        TvShow show = activity.getShow();
        final long showId = ShowWrapper.getShowId(getContentResolver(), show);

        Episode episode = activity.getEpisode();
        final long episodeId = EpisodeWrapper.getEpisodeId(resolver, episode);

        if (showId == -1L || episodeId == -1L) {
          queueTask(new SyncShowTask(show.getTvdbId()));
          queueTask(new SyncWatchingTask());
          postOnSuccess();
          return;
        }

        episodeWatching.remove(episodeId);

        switch (action) {
          case CHECKIN:
            op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
                .withValue(EpisodeColumns.CHECKED_IN, true)
                .build();
            break;

          case WATCHING:
            op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
                .withValue(EpisodeColumns.WATCHING, true)
                .build();
            break;
        }

        ops.add(op);
      } else if (type == ActivityType.MOVIE) {
        Movie movie = activity.getMovie();
        long movieId = MovieWrapper.getMovieId(resolver, movie);
        if (movieId == -1L) {
          movieId = MovieWrapper.updateOrInsertMovie(getContentResolver(), movie);
          queueTask(new SyncMovieTask(movie.getTmdbId()));
        }

        movieWatching.remove(movieId);

        switch (action) {
          case CHECKIN:
            op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
                .withValue(MovieColumns.CHECKED_IN, true)
                .build();
            break;

          case WATCHING:
            op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
                .withValue(MovieColumns.WATCHING, true)
                .build();
            break;
        }

        ops.add(op);
      }
    }

    for (Long episodeId : episodeWatching) {
      final int showTvdbId = EpisodeWrapper.getShowTvdbId(getContentResolver(), episodeId);
      queueTask(new SyncShowTask(showTvdbId));

      op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
          .withValue(EpisodeColumns.CHECKED_IN, false)
          .withValue(EpisodeColumns.WATCHING, false)
          .build();
      ops.add(op);
    }

    for (Long movieId : movieWatching) {
      final long tmdbId = MovieWrapper.getTmdbId(getContentResolver(), movieId);
      queueTask(new SyncMovieTask(tmdbId));

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
