/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.remote.upgrade;

import android.database.Cursor;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.schematic.Cursors;

public class EnsureSync extends Job {

  @Override public String key() {
    return "EnsureSync";
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean perform() {
    Cursor shows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.TRAKT_ID, ShowColumns.NEEDS_SYNC, ShowColumns.IN_WATCHLIST,
        ShowColumns.WATCHED_COUNT, ShowColumns.IN_COLLECTION_COUNT, ShowColumns.IN_WATCHLIST_COUNT,
    }, null, null, null);

    while (shows.moveToNext()) {
      final long traktId = Cursors.getLong(shows, ShowColumns.TRAKT_ID);
      final boolean needsSync = Cursors.getBoolean(shows, ShowColumns.NEEDS_SYNC);
      final int watchedCount = Cursors.getInt(shows, ShowColumns.WATCHED_COUNT);
      final int collectedCount = Cursors.getInt(shows, ShowColumns.IN_COLLECTION_COUNT);
      final int watchlistCount = Cursors.getInt(shows, ShowColumns.IN_WATCHLIST_COUNT);
      final boolean inWatchlist = Cursors.getBoolean(shows, ShowColumns.IN_WATCHLIST);

      if (needsSync) {
        if (watchedCount > 0 || collectedCount > 0 || watchlistCount > 0 || inWatchlist) {
          queue(new SyncShow(traktId));
        }
      }
    }

    shows.close();

    Cursor movies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.TRAKT_ID, MovieColumns.NEEDS_SYNC, MovieColumns.WATCHED,
        MovieColumns.IN_COLLECTION, MovieColumns.IN_WATCHLIST,
    }, null, null, null);

    while (movies.moveToNext()) {
      final long traktId = Cursors.getLong(movies, MovieColumns.TRAKT_ID);
      final boolean needsSync = Cursors.getBoolean(movies, MovieColumns.NEEDS_SYNC);
      final boolean watched = Cursors.getBoolean(movies, MovieColumns.WATCHED);
      final boolean collected = Cursors.getBoolean(movies, MovieColumns.IN_COLLECTION);
      final boolean inWatchlist = Cursors.getBoolean(movies, MovieColumns.IN_WATCHLIST);

      if (needsSync) {
        if (watched || collected || inWatchlist) {
          queue(new SyncMovie(traktId));
        }
      }
    }

    movies.close();

    return true;
  }
}
