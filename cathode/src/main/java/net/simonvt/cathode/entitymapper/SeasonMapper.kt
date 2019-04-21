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
import net.simonvt.cathode.entity.Season
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns

object SeasonMapper : MappedCursorLiveData.CursorMapper<Season> {

  override fun map(cursor: Cursor): Season? {
    return if (cursor.moveToFirst()) mapSeason(cursor) else null
  }

  fun mapSeason(cursor: Cursor): Season {
    val id = cursor.getLong(SeasonColumns.ID)
    val showId = cursor.getLong(SeasonColumns.SHOW_ID)
    val season = cursor.getInt(SeasonColumns.SEASON)
    val tvdbId = cursor.getInt(SeasonColumns.TVDB_ID)
    val tmdbId = cursor.getInt(SeasonColumns.TMDB_ID)
    val tvrageId = cursor.getLong(SeasonColumns.TVRAGE_ID)
    val userRating = cursor.getInt(SeasonColumns.USER_RATING)
    val ratedAt = cursor.getLong(SeasonColumns.RATED_AT)
    val rating = cursor.getFloat(SeasonColumns.RATING)
    val votes = cursor.getInt(SeasonColumns.VOTES)
    val hiddenWatched = cursor.getBoolean(SeasonColumns.HIDDEN_WATCHED)
    val hiddenCollected = cursor.getBoolean(SeasonColumns.HIDDEN_COLLECTED)
    val watchedCount = cursor.getInt(SeasonColumns.WATCHED_COUNT)
    val airdateCount = cursor.getInt(SeasonColumns.AIRDATE_COUNT)
    val inCollectionCount = cursor.getInt(SeasonColumns.IN_COLLECTION_COUNT)
    val inWatchlistCount = cursor.getInt(SeasonColumns.IN_WATCHLIST_COUNT)
    val needsSync = cursor.getBoolean(SeasonColumns.NEEDS_SYNC)
    val showTitle = cursor.getStringOrNull(SeasonColumns.SHOW_TITLE)
    val airedCount = cursor.getInt(SeasonColumns.AIRED_COUNT)
    val unairedCount = cursor.getInt(SeasonColumns.UNAIRED_COUNT)
    val watchedAiredCount = cursor.getInt(SeasonColumns.WATCHED_AIRED_COUNT)
    val collectedAiredCount = cursor.getInt(SeasonColumns.COLLECTED_AIRED_COUNT)
    val episodeCount = cursor.getInt(SeasonColumns.EPISODE_COUNT)

    return Season(
      id,
      showId,
      season,
      tvdbId,
      tmdbId,
      tvrageId,
      userRating,
      ratedAt,
      rating,
      votes,
      hiddenWatched,
      hiddenCollected,
      watchedCount,
      airdateCount,
      inCollectionCount,
      inWatchlistCount,
      needsSync,
      showTitle,
      airedCount,
      unairedCount,
      watchedAiredCount,
      collectedAiredCount,
      episodeCount
    )
  }

  val projection = arrayOf(
    SeasonColumns.ID,
    SeasonColumns.SHOW_ID,
    SeasonColumns.SEASON,
    SeasonColumns.TVDB_ID,
    SeasonColumns.TMDB_ID,
    SeasonColumns.TVRAGE_ID,
    SeasonColumns.USER_RATING,
    SeasonColumns.RATED_AT,
    SeasonColumns.RATING,
    SeasonColumns.VOTES,
    SeasonColumns.HIDDEN_WATCHED,
    SeasonColumns.HIDDEN_COLLECTED,
    SeasonColumns.WATCHED_COUNT,
    SeasonColumns.AIRDATE_COUNT,
    SeasonColumns.IN_COLLECTION_COUNT,
    SeasonColumns.IN_WATCHLIST_COUNT,
    SeasonColumns.NEEDS_SYNC,
    SeasonColumns.SHOW_TITLE,
    SeasonColumns.AIRED_COUNT,
    SeasonColumns.UNAIRED_COUNT,
    SeasonColumns.WATCHED_AIRED_COUNT,
    SeasonColumns.COLLECTED_AIRED_COUNT,
    SeasonColumns.EPISODE_COUNT
  )
}
