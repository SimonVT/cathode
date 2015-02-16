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
import android.database.Cursor;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.util.DateUtils;

import static net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import static net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import static net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import static net.simonvt.cathode.provider.ProviderSchematic.Seasons;

public class UpdateShowCounts extends Job {

  @Override public String key() {
    return "UpdateShowCounts";
  }

  @Override public void perform() {
    Cursor shows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, null, null, null);

    while (shows.moveToNext()) {
      final long showId = shows.getLong(shows.getColumnIndex(ShowColumns.ID));

      Cursor watched = getContentResolver().query(Episodes.fromShow(showId), new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.WATCHED + "=1 AND " + EpisodeColumns.NEEDS_SYNC + "=0", null, null);
      final int watchedCount = watched.getCount();
      watched.close();

      Cursor collected = getContentResolver().query(Episodes.fromShow(showId), new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.IN_COLLECTION + "=1 AND " + EpisodeColumns.NEEDS_SYNC + "=0", null, null);
      final int collectedCount = collected.getCount();
      collected.close();

      Cursor airdate = getContentResolver().query(Episodes.fromShow(showId), new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.FIRST_AIRED
          + ">"
          + DateUtils.YEAR_IN_MILLIS
          + " AND "
          + EpisodeColumns.NEEDS_SYNC
          + "=0", null, null);
      final int airdateCount = airdate.getCount();
      airdate.close();

      ContentValues values = new ContentValues();
      values.put(ShowColumns.WATCHED_COUNT, watchedCount);
      values.put(ShowColumns.IN_COLLECTION_COUNT, collectedCount);
      values.put(ShowColumns.AIRDATE_COUNT, airdateCount);

      getContentResolver().update(Shows.withId(showId), values, null, null);
    }

    shows.close();

    Cursor seasons = getContentResolver().query(Seasons.SEASONS, new String[] {
        ShowColumns.ID,
    }, null, null, null);

    while (seasons.moveToNext()) {
      final long seasonId = seasons.getLong(seasons.getColumnIndex(SeasonColumns.ID));

      Cursor watched = getContentResolver().query(Episodes.fromSeason(seasonId), new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.WATCHED + "=1 AND " + EpisodeColumns.NEEDS_SYNC + "=0", null, null);
      final int watchedCount = watched.getCount();
      watched.close();

      Cursor collected = getContentResolver().query(Episodes.fromSeason(seasonId), new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.IN_COLLECTION + "=1 AND " + EpisodeColumns.NEEDS_SYNC + "=0", null, null);
      final int collectedCount = collected.getCount();
      collected.close();

      Cursor airdate = getContentResolver().query(Episodes.fromSeason(seasonId), new String[] {
          EpisodeColumns.ID,
      }, EpisodeColumns.FIRST_AIRED
          + ">"
          + DateUtils.YEAR_IN_MILLIS
          + " AND "
          + EpisodeColumns.NEEDS_SYNC
          + "=0", null, null);
      final int airdateCount = airdate.getCount();
      airdate.close();

      ContentValues values = new ContentValues();
      values.put(SeasonColumns.WATCHED_COUNT, watchedCount);
      values.put(SeasonColumns.IN_COLLECTION_COUNT, collectedCount);
      values.put(SeasonColumns.AIRDATE_COUNT, airdateCount);

      getContentResolver().update(Seasons.withId(seasonId), values, null, null);
    }

    seasons.close();
  }
}
