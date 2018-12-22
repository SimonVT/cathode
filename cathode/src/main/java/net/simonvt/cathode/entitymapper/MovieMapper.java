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
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;

public class MovieMapper implements MappedCursorLiveData.CursorMapper<Movie> {

  @Override public Movie map(Cursor cursor) {
    if (cursor.moveToFirst()) {
      return mapMovie(cursor);
    }

    return null;
  }

  static Movie mapMovie(Cursor cursor) {
    long id = Cursors.getLong(cursor, MovieColumns.ID);
    Long traktId = Cursors.getLongOrNull(cursor, MovieColumns.TRAKT_ID);
    String imdbId = Cursors.getStringOrNull(cursor, MovieColumns.IMDB_ID);
    Integer tmdbId = Cursors.getIntOrNull(cursor, MovieColumns.TMDB_ID);
    String title = Cursors.getString(cursor, MovieColumns.TITLE);
    String titleNoArticle = Cursors.getStringOrNull(cursor, MovieColumns.TITLE_NO_ARTICLE);
    Integer year = Cursors.getIntOrNull(cursor, MovieColumns.YEAR);
    String released = Cursors.getStringOrNull(cursor, MovieColumns.RELEASED);
    Integer runtime = Cursors.getIntOrNull(cursor, MovieColumns.RUNTIME);
    String tagline = Cursors.getStringOrNull(cursor, MovieColumns.TAGLINE);
    String overview = Cursors.getStringOrNull(cursor, MovieColumns.OVERVIEW);
    String certification = Cursors.getStringOrNull(cursor, MovieColumns.CERTIFICATION);
    String language = Cursors.getStringOrNull(cursor, MovieColumns.LANGUAGE);
    String homepage = Cursors.getStringOrNull(cursor, MovieColumns.HOMEPAGE);
    String trailer = Cursors.getStringOrNull(cursor, MovieColumns.TRAILER);
    Integer userRating = Cursors.getIntOrNull(cursor, MovieColumns.USER_RATING);
    Float rating = Cursors.getFloatOrNull(cursor, MovieColumns.RATING);
    Integer votes = Cursors.getIntOrNull(cursor, MovieColumns.VOTES);
    Boolean watched = Cursors.getBooleanOrNull(cursor, MovieColumns.WATCHED);
    Long watchedAt = Cursors.getLongOrNull(cursor, MovieColumns.WATCHED_AT);
    Boolean inCollection = Cursors.getBooleanOrNull(cursor, MovieColumns.IN_COLLECTION);
    Long collectedAt = Cursors.getLongOrNull(cursor, MovieColumns.COLLECTED_AT);
    Boolean inWatchlist = Cursors.getBooleanOrNull(cursor, MovieColumns.IN_WATCHLIST);
    Long watchlistedAt = Cursors.getLongOrNull(cursor, MovieColumns.LISTED_AT);
    Boolean watching = Cursors.getBooleanOrNull(cursor, MovieColumns.WATCHING);
    Boolean checkedIn = Cursors.getBooleanOrNull(cursor, MovieColumns.CHECKED_IN);
    Long checkinStartedAt = Cursors.getLongOrNull(cursor, MovieColumns.STARTED_AT);
    Long checkinExpiresAt = Cursors.getLongOrNull(cursor, MovieColumns.EXPIRES_AT);
    Boolean needsSync = Cursors.getBooleanOrNull(cursor, MovieColumns.NEEDS_SYNC);
    Long lastSync = Cursors.getLongOrNull(cursor, MovieColumns.LAST_SYNC);
    Long lastCommentSync = Cursors.getLongOrNull(cursor, MovieColumns.LAST_COMMENT_SYNC);
    Long lastCreditsSync = Cursors.getLongOrNull(cursor, MovieColumns.LAST_CREDITS_SYNC);
    Long lastRelatedSync = Cursors.getLongOrNull(cursor, MovieColumns.LAST_RELATED_SYNC);

    return new Movie(id, traktId, imdbId, tmdbId, title, titleNoArticle, year, released, runtime,
        tagline, overview, certification, language, homepage, trailer, userRating, rating, votes,
        watched, watchedAt, inCollection, collectedAt, inWatchlist, watchlistedAt, watching,
        checkedIn, checkinStartedAt, checkinExpiresAt, needsSync, lastSync, lastCommentSync,
        lastCreditsSync, lastRelatedSync);
  }
}
