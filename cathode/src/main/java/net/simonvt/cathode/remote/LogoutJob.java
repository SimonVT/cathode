/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

import android.content.ContentValues;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public class LogoutJob extends Job {

  @Override public String key() {
    return "LogoutJob";
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public void perform() {
    ContentValues values;

    // Clear movie user data
    values = new ContentValues();

    values.put(MovieColumns.WATCHED, false);
    values.put(MovieColumns.WATCHED_AT, 0);

    values.put(MovieColumns.USER_RATING, 0);
    values.put(MovieColumns.RATED_AT, 0);

    values.put(MovieColumns.IN_COLLECTION, false);
    values.put(MovieColumns.COLLECTED_AT, 0);

    values.put(MovieColumns.IN_WATCHLIST, false);
    values.put(MovieColumns.LISTED_AT, 0);

    values.put(MovieColumns.WATCHING, false);
    values.put(MovieColumns.CHECKED_IN, false);
    values.put(MovieColumns.STARTED_AT, 0);
    values.put(MovieColumns.EXPIRES_AT, 0);

    getContentResolver().update(Movies.MOVIES, values, null, null);

    // Clear episode user data
    values = new ContentValues();

    values.put(EpisodeColumns.USER_RATING, 0);
    values.put(EpisodeColumns.RATED_AT, 0);

    values.put(EpisodeColumns.PLAYS, 0);

    values.put(EpisodeColumns.WATCHED, false);
    values.put(EpisodeColumns.IN_WATCHLIST, false);
    values.put(EpisodeColumns.IN_COLLECTION, false);
    values.put(EpisodeColumns.COLLECTED_AT, 0);
    values.put(EpisodeColumns.LISTED_AT, 0);

    values.put(EpisodeColumns.WATCHING, false);
    values.put(EpisodeColumns.CHECKED_IN, false);
    values.put(EpisodeColumns.STARTED_AT, 0);
    values.put(EpisodeColumns.EXPIRES_AT, 0);

    getContentResolver().update(Episodes.EPISODES, values, null, null);

    // Clear season user data
    values = new ContentValues();
    values.put(SeasonColumns.USER_RATING, 0);
    values.put(SeasonColumns.RATED_AT, 0);
    getContentResolver().update(Seasons.SEASONS, values, null, null);

    // Clear show user data
    values = new ContentValues();

    values.put(ShowColumns.USER_RATING, 0);
    values.put(ShowColumns.RATED_AT, 0);

    values.put(ShowColumns.IN_WATCHLIST, 0);
    values.put(ShowColumns.LISTED_AT, 0);

    values.put(ShowColumns.LAST_WATCHED_AT, 0);
    values.put(ShowColumns.LAST_COLLECTED_AT, 0);

    values.put(ShowColumns.HIDDEN, false);

    getContentResolver().update(Shows.SHOWS, values, null, null);
  }
}
