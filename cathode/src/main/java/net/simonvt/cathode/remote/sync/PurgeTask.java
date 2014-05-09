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
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.MovieActors;
import net.simonvt.cathode.provider.ProviderSchematic.MovieDirectors;
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres;
import net.simonvt.cathode.provider.ProviderSchematic.MovieProducers;
import net.simonvt.cathode.provider.ProviderSchematic.MovieTopWatchers;
import net.simonvt.cathode.provider.ProviderSchematic.MovieWriters;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.ShowActors;
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres;
import net.simonvt.cathode.provider.ProviderSchematic.ShowTopEpisodes;
import net.simonvt.cathode.provider.ProviderSchematic.ShowTopWatchers;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.MovieSearchHandler;
import net.simonvt.cathode.util.ShowSearchHandler;
import timber.log.Timber;

public class PurgeTask extends TraktTask {

  @Override protected void doTask() {

    String showsWhere = ShowColumns.WATCHED_COUNT
        + "=0 AND "
        + ShowColumns.IN_COLLECTION_COUNT
        + "=0 AND "
        + ShowColumns.IN_WATCHLIST_COUNT
        + "=0 AND "
        + ShowColumns.IN_WATCHLIST
        + "=0 AND "
        + Shows.getWatchingQuery()
        + "=0 AND "
        + ShowColumns.RECOMMENDATION_INDEX
        + "=-1 AND "
        + ShowColumns.TRENDING_INDEX
        + "=-1";

    Cursor shows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID, ShowColumns.TITLE,
    }, showsWhere, null, null);

    List<Long> showIds = new ArrayList<Long>();
    while (shows.moveToNext()) {
      final long id = shows.getLong(shows.getColumnIndex(ShowColumns.ID));
      final String title = shows.getString(shows.getColumnIndex(ShowColumns.TITLE));
      Timber.d("Purging " + title);
      showIds.add(id);
    }

    List<Long> showSearchIds = ShowSearchHandler.showIds;
    if (showSearchIds != null) {
      for (Long id : showSearchIds) {
        showIds.remove(id);
      }
    }

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    for (Long id : showIds) {
      ContentProviderOperation op =
          ContentProviderOperation.newDelete(Episodes.fromShow(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(Seasons.fromShow(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(ShowTopWatchers.fromShow(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(ShowTopEpisodes.fromShow(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(ShowActors.fromShow(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(ShowGenres.fromShow(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(Shows.withId(id)).build();
      ops.add(op);
    }

    try {
      getContentResolver().applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Batch delete failed");
    } catch (OperationApplicationException e) {
      Timber.e(e, "Batch delete failed");
    }

    String moviesWhere = MovieColumns.WATCHED
        + "=0 AND "
        + MovieColumns.IN_COLLECTION
        + "=0 AND "
        + MovieColumns.IN_WATCHLIST
        + "=0 AND "
        + MovieColumns.WATCHING
        + "=0 AND "
        + MovieColumns.RECOMMENDATION_INDEX
        + "=-1 AND "
        + MovieColumns.TRENDING_INDEX
        + "=-1";

    Cursor movies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID, MovieColumns.TITLE,
    }, moviesWhere, null, null);

    List<Long> movieIds = new ArrayList<Long>();
    while (movies.moveToNext()) {
      final long id = movies.getLong(movies.getColumnIndex(MovieColumns.ID));
      final String title = movies.getString(movies.getColumnIndex(MovieColumns.TITLE));
      Timber.d("Purging " + title);
      movieIds.add(id);
    }

    List<Long> movieSearchIds = MovieSearchHandler.movieIds;
    if (movieSearchIds != null) {
      for (Long id : movieSearchIds) {
        movieIds.remove(id);
      }
    }

    ops = new ArrayList<ContentProviderOperation>();
    for (Long id : movieIds) {
      ContentProviderOperation op = ContentProviderOperation.newDelete(Movies.withId(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(MovieGenres.fromMovie(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(MovieTopWatchers.fromMovie(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(MovieActors.fromMovie(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(MovieDirectors.fromMovie(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(MovieWriters.fromMovie(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(MovieProducers.fromMovie(id)).build();
      ops.add(op);
    }

    try {
      getContentResolver().applyBatch(BuildConfig.PROVIDER_AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Batch delete failed");
    } catch (OperationApplicationException e) {
      Timber.e(e, "Batch delete failed");
    }

    postOnSuccess();
  }
}
