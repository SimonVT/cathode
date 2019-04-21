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
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.util.DataHelper

object EpisodeMapper : MappedCursorLiveData.CursorMapper<Episode> {

  override fun map(cursor: Cursor): Episode? {
    return if (cursor.moveToFirst()) mapEpisode(cursor) else null
  }

  fun mapEpisode(cursor: Cursor): Episode {
    val id = cursor.getLong(EpisodeColumns.ID)
    val showId = cursor.getLong(EpisodeColumns.SHOW_ID)
    val seasonId = cursor.getLong(EpisodeColumns.SEASON_ID)
    val season = cursor.getInt(EpisodeColumns.SEASON)
    val episode = cursor.getInt(EpisodeColumns.EPISODE)
    val numberAbs = cursor.getInt(EpisodeColumns.NUMBER_ABS)
    val title = cursor.getStringOrNull(EpisodeColumns.TITLE)
    val overview = cursor.getStringOrNull(EpisodeColumns.OVERVIEW)
    val traktId = cursor.getLong(EpisodeColumns.TRAKT_ID)
    val imdbId = cursor.getStringOrNull(EpisodeColumns.IMDB_ID)
    val tvdbId = cursor.getInt(EpisodeColumns.TVDB_ID)
    val tmdbId = cursor.getInt(EpisodeColumns.TMDB_ID)
    val tvrageId = cursor.getLong(EpisodeColumns.TVRAGE_ID)
    var firstAired = cursor.getLong(EpisodeColumns.FIRST_AIRED)
    if (firstAired != 0L) {
      firstAired = DataHelper.getFirstAired(firstAired)
    }
    val updatedAt = cursor.getLong(EpisodeColumns.UPDATED_AT)
    val userRating = cursor.getInt(EpisodeColumns.USER_RATING)
    val ratedAt = cursor.getLong(EpisodeColumns.RATED_AT)
    val rating = cursor.getFloat(EpisodeColumns.RATING)
    val votes = cursor.getInt(EpisodeColumns.VOTES)
    val plays = cursor.getInt(EpisodeColumns.PLAYS)
    val watched = cursor.getBoolean(EpisodeColumns.WATCHED)
    val watchedAt = cursor.getLong(EpisodeColumns.LAST_WATCHED_AT)
    val inCollection = cursor.getBoolean(EpisodeColumns.IN_COLLECTION)
    val collectedAt = cursor.getLong(EpisodeColumns.COLLECTED_AT)
    val inWatchlist = cursor.getBoolean(EpisodeColumns.IN_WATCHLIST)
    val watchlistedAt = cursor.getLong(EpisodeColumns.LISTED_AT)
    val watching = cursor.getBoolean(EpisodeColumns.WATCHING)
    val checkedIn = cursor.getBoolean(EpisodeColumns.CHECKED_IN)
    val checkinStartedAt = cursor.getLong(EpisodeColumns.STARTED_AT)
    val checkinExpiresAt = cursor.getLong(EpisodeColumns.EXPIRES_AT)
    val lastCommentSync = cursor.getLong(EpisodeColumns.LAST_COMMENT_SYNC)
    val notificationDismissed = cursor.getBoolean(EpisodeColumns.NOTIFICATION_DISMISSED)
    val showTitle = cursor.getStringOrNull(EpisodeColumns.SHOW_TITLE)

    return Episode(
      id,
      showId,
      seasonId,
      season,
      episode,
      numberAbs,
      title,
      overview,
      traktId,
      imdbId,
      tvdbId,
      tmdbId,
      tvrageId,
      firstAired,
      updatedAt,
      userRating,
      ratedAt,
      rating,
      votes,
      plays,
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
      lastCommentSync,
      notificationDismissed,
      showTitle
    )
  }

  @JvmField
  val projection = arrayOf(
    EpisodeColumns.ID,
    EpisodeColumns.SHOW_ID,
    EpisodeColumns.SEASON_ID,
    EpisodeColumns.SEASON,
    EpisodeColumns.EPISODE,
    EpisodeColumns.NUMBER_ABS,
    EpisodeColumns.TITLE,
    EpisodeColumns.OVERVIEW,
    EpisodeColumns.TRAKT_ID,
    EpisodeColumns.TVDB_ID,
    EpisodeColumns.TMDB_ID,
    EpisodeColumns.TVRAGE_ID,
    EpisodeColumns.FIRST_AIRED,
    EpisodeColumns.UPDATED_AT,
    EpisodeColumns.USER_RATING,
    EpisodeColumns.RATED_AT,
    EpisodeColumns.RATING,
    EpisodeColumns.VOTES,
    EpisodeColumns.PLAYS,
    EpisodeColumns.WATCHED,
    EpisodeColumns.LAST_WATCHED_AT,
    EpisodeColumns.IN_COLLECTION,
    EpisodeColumns.COLLECTED_AT,
    EpisodeColumns.IN_WATCHLIST,
    EpisodeColumns.LISTED_AT,
    EpisodeColumns.WATCHING,
    EpisodeColumns.CHECKED_IN,
    EpisodeColumns.STARTED_AT,
    EpisodeColumns.EXPIRES_AT,
    EpisodeColumns.LAST_COMMENT_SYNC,
    EpisodeColumns.NOTIFICATION_DISMISSED,
    EpisodeColumns.SHOW_TITLE
  )
}
