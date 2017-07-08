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

package net.simonvt.cathode.remote;

import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.schematic.Cursors;

public class ForceUpdateJob extends Job {

  @Inject transient ShowDatabaseHelper showHelper;

  @Override public String key() {
    return "ForceUpdateJob";
  }

  @Override public int getPriority() {
    return JobPriority.ACTIONS;
  }

  @Override public boolean perform() {
    Cursor shows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID, ShowColumns.TRAKT_ID,
    }, null, null, null);

    while (shows.moveToNext()) {
      final long traktId = Cursors.getLong(shows, ShowColumns.TRAKT_ID);
      queue(new SyncShow(traktId));
    }

    shows.close();

    Cursor movies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.TRAKT_ID,
    }, null, null, null);

    while (movies.moveToNext()) {
      final long traktId = Cursors.getLong(movies, MovieColumns.TRAKT_ID);
      queue(new SyncMovie(traktId));
    }

    movies.close();

    return true;
  }
}
