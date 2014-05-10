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
import net.simonvt.cathode.api.entity.ActivityItem;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.enumeration.ActivityAction;
import net.simonvt.cathode.api.enumeration.ActivityType;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeDatabase.Tables;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

import static net.simonvt.cathode.provider.CathodeContract.Episodes;
import static net.simonvt.cathode.provider.CathodeContract.Movies;

public class SyncWatchingTask extends TraktTask {

  @Inject transient UserService userService;

  @Override protected void doTask() {
    ContentResolver resolver = getContentResolver();

    ActivityItem activity = userService.watching();

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    Cursor episodeWatchingCursor =
        getContentResolver().query(Episodes.EPISODE_WATCHING, new String[] {
            Tables.EPISODES + "." + Episodes._ID,
        }, null, null, null);

    List<Long> episodeWatching = new ArrayList<Long>();
    while (episodeWatchingCursor.moveToNext()) {
      episodeWatching.add(
          episodeWatchingCursor.getLong(episodeWatchingCursor.getColumnIndex(Episodes._ID)));
    }
    episodeWatchingCursor.close();

    Cursor movieWatchingCursor = getContentResolver().query(Movies.MOVIE_WATCHING, new String[] {
        Movies._ID,
    }, null, null, null);

    List<Long> movieWatching = new ArrayList<Long>();
    while (movieWatchingCursor.moveToNext()) {
      movieWatching.add(
          movieWatchingCursor.getLong(movieWatchingCursor.getColumnIndex(Movies._ID)));
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
            op = ContentProviderOperation.newUpdate(Episodes.buildFromId(episodeId))
                .withValue(Episodes.CHECKED_IN, true)
                .build();
            break;

          case WATCHING:
            op = ContentProviderOperation.newUpdate(Episodes.buildFromId(episodeId))
                .withValue(Episodes.WATCHING, true)
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
            op = ContentProviderOperation.newUpdate(Movies.buildFromId(movieId))
                .withValue(Movies.CHECKED_IN, true)
                .build();
            break;

          case WATCHING:
            op = ContentProviderOperation.newUpdate(Movies.buildFromId(movieId))
                .withValue(Movies.WATCHING, true)
                .build();
            break;
        }

        ops.add(op);
      }
    }

    for (Long episodeId : episodeWatching) {
      final int showTvdbId = EpisodeWrapper.getShowTvdbId(getContentResolver(), episodeId);
      queueTask(new SyncShowTask(showTvdbId));

      op = ContentProviderOperation.newUpdate(Episodes.buildFromId(episodeId))
          .withValue(Episodes.CHECKED_IN, false)
          .withValue(Episodes.WATCHING, false)
          .build();
      ops.add(op);
    }

    for (Long movieId : movieWatching) {
      final long tmdbId = MovieWrapper.getTmdbId(getContentResolver(), movieId);
      queueTask(new SyncMovieTask(tmdbId));

      op = ContentProviderOperation.newUpdate(Movies.buildFromId(movieId))
          .withValue(Movies.CHECKED_IN, false)
          .withValue(Movies.WATCHING, false)
          .build();
      ops.add(op);
    }

    try {
      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "SyncWatchingTask failed");
    } catch (OperationApplicationException e) {
      Timber.e(e, "SyncWatchingTask failed");
    }

    postOnSuccess();
  }
}
