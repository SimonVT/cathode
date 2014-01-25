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
import android.os.RemoteException;
import java.util.ArrayList;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.ActivityItem;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.enumeration.ActivityAction;
import net.simonvt.cathode.api.enumeration.ActivityType;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.MovieWrapper;
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

    ContentProviderOperation op = ContentProviderOperation.newUpdate(Episodes.EPISODE_WATCHING)
        .withValue(Episodes.WATCHING, false)
        .withValue(Episodes.CHECKED_IN, false)
        .build();
    ops.add(op);

    op = ContentProviderOperation.newUpdate(Movies.MOVIE_WATCHING)
        .withValue(Movies.WATCHING, false)
        .withValue(Movies.CHECKED_IN, false)
        .build();
    ops.add(op);

    if (activity != null) {
      ActivityType type = activity.getType();
      ActivityAction action = activity.getAction();

      if (type == ActivityType.EPISODE) {
        Episode episode = activity.getEpisode();
        final long episodeId = EpisodeWrapper.getEpisodeId(resolver, episode);

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
        final long movieId = MovieWrapper.getMovieId(resolver, movie);

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

    try {
      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, null);
    } catch (OperationApplicationException e) {
      Timber.e(e, null);
    }

    postOnSuccess();
  }
}
