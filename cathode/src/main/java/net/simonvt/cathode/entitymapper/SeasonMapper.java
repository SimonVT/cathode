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
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.entity.Season;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;

public class SeasonMapper implements MappedCursorLiveData.CursorMapper<Season> {

  @Override public Season map(Cursor cursor) {
    if (cursor.moveToFirst()) {
      return mapSeason(cursor);
    }

    return null;
  }

  public static Season mapSeason(Cursor cursor) {
    long id = Cursors.getLong(cursor, SeasonColumns.ID);
    Long showId = Cursors.getLongOrNull(cursor, SeasonColumns.SHOW_ID);
    int season = Cursors.getInt(cursor, SeasonColumns.SEASON);
    Integer tvdbId = Cursors.getIntOrNull(cursor, SeasonColumns.TVDB_ID);
    Integer tmdbId = Cursors.getIntOrNull(cursor, SeasonColumns.TMDB_ID);
    Long tvrageId = Cursors.getLongOrNull(cursor, SeasonColumns.TVRAGE_ID);
    Integer userRating = Cursors.getIntOrNull(cursor, SeasonColumns.USER_RATING);
    Long ratedAt = Cursors.getLongOrNull(cursor, SeasonColumns.RATED_AT);
    Float rating = Cursors.getFloatOrNull(cursor, SeasonColumns.RATING);
    Integer votes = Cursors.getIntOrNull(cursor, SeasonColumns.VOTES);
    Boolean hiddenWatched = Cursors.getBooleanOrNull(cursor, SeasonColumns.HIDDEN_WATCHED);
    Boolean hiddenCollected = Cursors.getBooleanOrNull(cursor, SeasonColumns.HIDDEN_COLLECTED);
    Integer watchedCount = Cursors.getIntOrNull(cursor, SeasonColumns.WATCHED_COUNT);
    Integer airdateCount = Cursors.getIntOrNull(cursor, SeasonColumns.AIRDATE_COUNT);
    Integer inCollectionCount = Cursors.getIntOrNull(cursor, SeasonColumns.IN_COLLECTION_COUNT);
    Integer inWatchlistCount = Cursors.getIntOrNull(cursor, SeasonColumns.IN_WATCHLIST_COUNT);
    Boolean needsSync = Cursors.getBooleanOrNull(cursor, SeasonColumns.NEEDS_SYNC);
    String showTitle = Cursors.getStringOrNull(cursor, SeasonColumns.SHOW_TITLE);
    Integer airedCount = Cursors.getIntOrNull(cursor, SeasonColumns.AIRED_COUNT);
    Integer unairedCount = Cursors.getIntOrNull(cursor, SeasonColumns.UNAIRED_COUNT);
    Integer watchedAiredCount = Cursors.getIntOrNull(cursor, SeasonColumns.WATCHED_AIRED_COUNT);
    Integer collectedAiredCount = Cursors.getIntOrNull(cursor, SeasonColumns.COLLECTED_AIRED_COUNT);
    Integer episodeCount = Cursors.getIntOrNull(cursor, SeasonColumns.EPISODE_COUNT);

    return new Season(id, showId, season, tvdbId, tmdbId, tvrageId, userRating, ratedAt, rating,
        votes, hiddenWatched, hiddenCollected, watchedCount, airdateCount, inCollectionCount,
        inWatchlistCount, needsSync, showTitle, airedCount, unairedCount, watchedAiredCount,
        collectedAiredCount, episodeCount);
  }
}
