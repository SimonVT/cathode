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

package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getFloat
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getStringOrNull
import net.simonvt.cathode.entity.Movie
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables

object MovieMapper : MappedCursorLiveData.CursorMapper<Movie> {

  override fun map(cursor: Cursor): Movie? {
    return if (cursor.moveToFirst()) {
      mapMovie(cursor)
    } else null
  }

  fun mapMovie(cursor: Cursor): Movie {
    val id = cursor.getLong(MovieColumns.ID)
    val traktId = cursor.getLong(MovieColumns.TRAKT_ID)
    val imdbId = cursor.getStringOrNull(MovieColumns.IMDB_ID)
    val tmdbId = cursor.getInt(MovieColumns.TMDB_ID)
    val title = cursor.getStringOrNull(MovieColumns.TITLE)
    val titleNoArticle = cursor.getStringOrNull(MovieColumns.TITLE_NO_ARTICLE)
    val year = cursor.getInt(MovieColumns.YEAR)
    val released = cursor.getStringOrNull(MovieColumns.RELEASED)
    val runtime = cursor.getInt(MovieColumns.RUNTIME)
    val tagline = cursor.getStringOrNull(MovieColumns.TAGLINE)
    val overview = cursor.getStringOrNull(MovieColumns.OVERVIEW)
    val certification = cursor.getStringOrNull(MovieColumns.CERTIFICATION)
    val language = cursor.getStringOrNull(MovieColumns.LANGUAGE)
    val homepage = cursor.getStringOrNull(MovieColumns.HOMEPAGE)
    val trailer = cursor.getStringOrNull(MovieColumns.TRAILER)
    val userRating = cursor.getInt(MovieColumns.USER_RATING)
    val rating = cursor.getFloat(MovieColumns.RATING)
    val votes = cursor.getInt(MovieColumns.VOTES)
    val watched = cursor.getBoolean(MovieColumns.WATCHED)
    val watchedAt = cursor.getLong(MovieColumns.WATCHED_AT)
    val inCollection = cursor.getBoolean(MovieColumns.IN_COLLECTION)
    val collectedAt = cursor.getLong(MovieColumns.COLLECTED_AT)
    val inWatchlist = cursor.getBoolean(MovieColumns.IN_WATCHLIST)
    val watchlistedAt = cursor.getLong(MovieColumns.LISTED_AT)
    val watching = cursor.getBoolean(MovieColumns.WATCHING)
    val checkedIn = cursor.getBoolean(MovieColumns.CHECKED_IN)
    val checkinStartedAt = cursor.getLong(MovieColumns.STARTED_AT)
    val checkinExpiresAt = cursor.getLong(MovieColumns.EXPIRES_AT)
    val needsSync = cursor.getBoolean(MovieColumns.NEEDS_SYNC)
    val lastSync = cursor.getLong(MovieColumns.LAST_SYNC)
    val lastCommentSync = cursor.getLong(MovieColumns.LAST_COMMENT_SYNC)
    val lastCreditsSync = cursor.getLong(MovieColumns.LAST_CREDITS_SYNC)
    val lastRelatedSync = cursor.getLong(MovieColumns.LAST_RELATED_SYNC)

    return Movie(
      id,
      traktId,
      imdbId,
      tmdbId,
      title,
      titleNoArticle,
      year,
      released,
      runtime,
      tagline,
      overview,
      certification,
      language,
      homepage,
      trailer,
      userRating,
      rating,
      votes,
      watched,
      watchedAt,
      inCollection,
      collectedAt,
      inWatchlist,
      watchlistedAt,
      watching,
      checkedIn,
      checkinStartedAt,
      checkinExpiresAt,
      needsSync,
      lastSync,
      lastCommentSync,
      lastCreditsSync,
      lastRelatedSync
    )
  }

  @JvmField
  val projection = arrayOf(
    Tables.MOVIES + "." + MovieColumns.ID,
    MovieColumns.TRAKT_ID,
    MovieColumns.IMDB_ID,
    MovieColumns.TMDB_ID,
    MovieColumns.TITLE,
    MovieColumns.TITLE_NO_ARTICLE,
    MovieColumns.YEAR,
    MovieColumns.RELEASED,
    MovieColumns.RUNTIME,
    MovieColumns.TAGLINE,
    MovieColumns.OVERVIEW,
    MovieColumns.CERTIFICATION,
    MovieColumns.LANGUAGE,
    MovieColumns.HOMEPAGE,
    MovieColumns.TRAILER,
    MovieColumns.USER_RATING,
    MovieColumns.RATING,
    MovieColumns.VOTES,
    MovieColumns.WATCHED,
    MovieColumns.WATCHED_AT,
    MovieColumns.IN_COLLECTION,
    MovieColumns.COLLECTED_AT,
    MovieColumns.IN_WATCHLIST,
    MovieColumns.LISTED_AT,
    MovieColumns.WATCHING,
    MovieColumns.CHECKED_IN,
    MovieColumns.STARTED_AT,
    MovieColumns.EXPIRES_AT,
    MovieColumns.NEEDS_SYNC,
    MovieColumns.LAST_SYNC,
    MovieColumns.LAST_COMMENT_SYNC,
    MovieColumns.LAST_CREDITS_SYNC,
    MovieColumns.LAST_RELATED_SYNC
  )
}
