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
import net.simonvt.cathode.provider.CathodeContract.Episodes;
import net.simonvt.cathode.provider.CathodeContract.Movies;
import net.simonvt.cathode.provider.CathodeContract.Seasons;
import net.simonvt.cathode.provider.CathodeContract.Shows;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.util.MovieSearchHandler;
import net.simonvt.cathode.util.ShowSearchHandler;
import timber.log.Timber;

public class PurgeTask extends TraktTask {

  @Override protected void doTask() {

    String showsWhere = Shows.WATCHED_COUNT
        + "=0 AND "
        + Shows.IN_COLLECTION_COUNT
        + "=0 AND "
        + Shows.IN_WATCHLIST_COUNT
        + "=0 AND "
        + Shows.IN_WATCHLIST
        + "=0 AND "
        + Shows.getWatchingQuery()
        + "=0 AND "
        + Shows.RECOMMENDATION_INDEX
        + "=-1 AND "
        + Shows.TRENDING_INDEX
        + "=-1";

    Cursor shows = getContentResolver().query(Shows.CONTENT_URI, new String[] {
        Shows._ID, Shows.TITLE,
    }, showsWhere, null, null);

    List<Long> showIds = new ArrayList<Long>();
    while (shows.moveToNext()) {
      final long id = shows.getLong(shows.getColumnIndex(Shows._ID));
      final String title = shows.getString(shows.getColumnIndex(Shows.TITLE));
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
          ContentProviderOperation.newDelete(Episodes.buildFromShowId(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(Seasons.buildFromShowId(id)).build();
      ops.add(op);
      op = ContentProviderOperation.newDelete(Shows.buildFromId(id)).build();
      ops.add(op);
    }

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Batch delete failed");
    } catch (OperationApplicationException e) {
      Timber.e(e, "Batch delete failed");
    }

    String moviesWhere = Movies.WATCHED
        + "=0 AND "
        + Movies.IN_COLLECTION
        + "=0 AND "
        + Movies.IN_WATCHLIST
        + "=0 AND "
        + Movies.WATCHING
        + "=0 AND "
        + Movies.RECOMMENDATION_INDEX
        + "=-1 AND "
        + Movies.TRENDING_INDEX
        + "=-1";

    Cursor movies = getContentResolver().query(Movies.CONTENT_URI, new String[] {
        Movies._ID, Movies.TITLE,
    }, moviesWhere, null, null);

    List<Long> movieIds = new ArrayList<Long>();
    while (movies.moveToNext()) {
      final long id = movies.getLong(movies.getColumnIndex(Movies._ID));
      final String title = movies.getString(movies.getColumnIndex(Movies.TITLE));
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
      ContentProviderOperation op =
          ContentProviderOperation.newDelete(Movies.buildFromId(id)).build();
      ops.add(op);
    }

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Batch delete failed");
    } catch (OperationApplicationException e) {
      Timber.e(e, "Batch delete failed");
    }

    postOnSuccess();
  }
}
