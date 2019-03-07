/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import android.text.TextUtils;
import net.simonvt.cathode.api.enumeration.ShowStatus;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.entity.ShowWithEpisode;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.util.DataHelper;

public class ShowWithEpisodeMapper implements MappedCursorLiveData.CursorMapper<ShowWithEpisode> {

  private static final String COLUMN_EPISODE_ID = "episodeId";

  public static final String[] PROJECTION = new String[] {
      Tables.SHOWS + "." + ShowColumns.ID, Tables.SHOWS + "." + ShowColumns.TITLE,
      Tables.SHOWS + "." + ShowColumns.OVERVIEW, ShowColumns.AIRED_COUNT,
      Tables.SHOWS + "." + ShowColumns.WATCHED_COUNT,
      Tables.SHOWS + "." + ShowColumns.IN_COLLECTION_COUNT, Tables.SHOWS + "." + ShowColumns.STATUS,
      ShowColumns.WATCHING, ShowColumns.WATCHING_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS " + COLUMN_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE, Tables.EPISODES + "." + EpisodeColumns.WATCHED,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON, Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.STARTED_AT,
      Tables.EPISODES + "." + EpisodeColumns.EXPIRES_AT,
  };

  @Override public ShowWithEpisode map(Cursor cursor) {
    if (cursor.moveToFirst()) {
      return mapShowAndEpisode(cursor);
    }

    return null;
  }

  public static ShowWithEpisode mapShowAndEpisode(Cursor cursor) {
    final long showId = Cursors.getLong(cursor, ShowColumns.ID);
    final String showTitle = Cursors.getString(cursor, ShowColumns.TITLE);
    final String showOverview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
    final int airedCount = Cursors.getInt(cursor, ShowColumns.AIRED_COUNT);
    final int watchedCount = Cursors.getInt(cursor, ShowColumns.WATCHED_COUNT);
    final int collectedCount = Cursors.getInt(cursor, ShowColumns.IN_COLLECTION_COUNT);
    final String statusString = Cursors.getString(cursor, ShowColumns.STATUS);
    ShowStatus status = null;
    if (!TextUtils.isEmpty(statusString)) {
      status = ShowStatus.fromValue(statusString);
    }
    boolean watching = Cursors.getBoolean(cursor, ShowColumns.WATCHING);
    long watchingEpisodeId = Cursors.getLong(cursor, ShowColumns.WATCHING_EPISODE_ID);
    Show show =
        new Show(showId, showTitle, null, null, null, null, showOverview, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, status, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            watchedCount, null, collectedCount, null, null, null, null, null, null, watching,
            airedCount, null, null, watchingEpisodeId);

    long episodeId = Cursors.getLong(cursor, COLUMN_EPISODE_ID);
    String episodeTitle = Cursors.getString(cursor, EpisodeColumns.TITLE);
    boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
    long firstAired = Cursors.getLong(cursor, EpisodeColumns.FIRST_AIRED);
    if (firstAired != 0L) {
      firstAired = DataHelper.getFirstAired(firstAired);
    }
    int season = Cursors.getInt(cursor, EpisodeColumns.SEASON);
    int number = Cursors.getInt(cursor, EpisodeColumns.EPISODE);
    long checkinStartedAt = Cursors.getLong(cursor, EpisodeColumns.STARTED_AT);
    long checkinExpiresAt = Cursors.getLong(cursor, EpisodeColumns.EXPIRES_AT);
    Episode episode =
        new Episode(episodeId, showId, null, season, number, null, episodeTitle, null, null, null,
            null, null, null, firstAired, null, null, null, null, null, null, watched, null, null,
            null, null, null, null, null, checkinStartedAt, checkinExpiresAt, null, null, null);

    return new ShowWithEpisode(show, episode);
  }
}
