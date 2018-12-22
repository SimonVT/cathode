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
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.util.DataHelper;

public class EpisodeMapper implements MappedCursorLiveData.CursorMapper<Episode> {

  @Override public Episode map(Cursor cursor) {
    if (cursor.moveToFirst()) {
      return mapEpisode(cursor);
    }

    return null;
  }

  public static Episode mapEpisode(Cursor cursor) {
    long id = Cursors.getLong(cursor, EpisodeColumns.ID);
    Long showId = Cursors.getLongOrNull(cursor, EpisodeColumns.SHOW_ID);
    Long seasonId = Cursors.getLongOrNull(cursor, EpisodeColumns.SEASON_ID);
    Integer season = Cursors.getIntOrNull(cursor, EpisodeColumns.SEASON);
    Integer episode = Cursors.getIntOrNull(cursor, EpisodeColumns.EPISODE);
    Integer numberAbs = Cursors.getIntOrNull(cursor, EpisodeColumns.NUMBER_ABS);
    String title = Cursors.getStringOrNull(cursor, EpisodeColumns.TITLE);
    String overview = Cursors.getStringOrNull(cursor, EpisodeColumns.OVERVIEW);
    Long traktId = Cursors.getLongOrNull(cursor, EpisodeColumns.TRAKT_ID);
    String imdbId = Cursors.getStringOrNull(cursor, EpisodeColumns.IMDB_ID);
    Integer tvdbId = Cursors.getIntOrNull(cursor, EpisodeColumns.TVDB_ID);
    Integer tmdbId = Cursors.getIntOrNull(cursor, EpisodeColumns.TMDB_ID);
    Long tvrageId = Cursors.getLongOrNull(cursor, EpisodeColumns.TVRAGE_ID);
    Long firstAired = Cursors.getLongOrNull(cursor, EpisodeColumns.FIRST_AIRED);
    if (firstAired == null) {
      firstAired = 0L;
    } else if (firstAired != 0L) {
      firstAired = DataHelper.getFirstAired(firstAired);
    }
    Long updatedAt = Cursors.getLongOrNull(cursor, EpisodeColumns.UPDATED_AT);
    Integer userRating = Cursors.getIntOrNull(cursor, EpisodeColumns.USER_RATING);
    Long ratedAt = Cursors.getLongOrNull(cursor, EpisodeColumns.RATED_AT);
    Float rating = Cursors.getFloatOrNull(cursor, EpisodeColumns.RATING);
    Integer votes = Cursors.getIntOrNull(cursor, EpisodeColumns.VOTES);
    Integer plays = Cursors.getIntOrNull(cursor, EpisodeColumns.PLAYS);
    Boolean watched = Cursors.getBooleanOrNull(cursor, EpisodeColumns.WATCHED);
    Long watchedAt = Cursors.getLongOrNull(cursor, EpisodeColumns.LAST_WATCHED_AT);
    Boolean inCollection = Cursors.getBooleanOrNull(cursor, EpisodeColumns.IN_COLLECTION);
    Long collectedAt = Cursors.getLongOrNull(cursor, EpisodeColumns.COLLECTED_AT);
    Boolean inWatchlist = Cursors.getBooleanOrNull(cursor, EpisodeColumns.IN_WATCHLIST);
    Long watchedlistedAt = Cursors.getLongOrNull(cursor, EpisodeColumns.LISTED_AT);
    Boolean watching = Cursors.getBooleanOrNull(cursor, EpisodeColumns.WATCHING);
    Boolean checkedIn = Cursors.getBooleanOrNull(cursor, EpisodeColumns.CHECKED_IN);
    Long checkinStartedAt = Cursors.getLongOrNull(cursor, EpisodeColumns.STARTED_AT);
    Long checkinExpiresAt = Cursors.getLongOrNull(cursor, EpisodeColumns.EXPIRES_AT);
    Long lastCommentSync = Cursors.getLongOrNull(cursor, EpisodeColumns.LAST_COMMENT_SYNC);
    Boolean notificationDismissed =
        Cursors.getBooleanOrNull(cursor, EpisodeColumns.NOTIFICATION_DISMISSED);
    String showTitle = Cursors.getStringOrNull(cursor, EpisodeColumns.SHOW_TITLE);

    return new Episode(id, showId, seasonId, season, episode, numberAbs, title, overview, traktId,
        imdbId, tvdbId, tmdbId, tvrageId, firstAired, updatedAt, userRating, ratedAt, rating, votes,
        plays, watched, watchedAt, inCollection, collectedAt, inWatchlist, watchedlistedAt,
        watching, checkedIn, checkinStartedAt, checkinExpiresAt, lastCommentSync,
        notificationDismissed, showTitle);
  }
}
