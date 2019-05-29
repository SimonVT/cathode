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

package net.simonvt.cathode.provider.helper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import net.simonvt.cathode.api.entity.Season
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.update
import net.simonvt.cathode.settings.FirstAiredOffsetPreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeasonDatabaseHelper @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper
) {

  fun getId(showId: Long, season: Int): Long {
    synchronized(LOCK_ID) {
      var c: Cursor? = null
      try {
        c = context.contentResolver.query(
          Seasons.SEASONS,
          arrayOf(SeasonColumns.ID),
          SeasonColumns.SHOW_ID + "=? AND " + SeasonColumns.SEASON + "=?",
          arrayOf(showId.toString(), season.toString())
        )
        return if (!c.moveToFirst()) -1L else c.getLong(SeasonColumns.ID)
      } finally {
        c?.close()
      }
    }
  }

  fun getTmdbId(seasonId: Long): Int {
    synchronized(LOCK_ID) {
      var c: Cursor? = null
      try {
        c = context.contentResolver.query(Seasons.withId(seasonId), arrayOf(SeasonColumns.TMDB_ID))
        return if (!c.moveToFirst()) -1 else c.getInt(SeasonColumns.TMDB_ID)
      } finally {
        c?.close()
      }
    }
  }

  fun getNumber(seasonId: Long): Int {
    var c: Cursor? = null
    try {
      c = context.contentResolver.query(
        Seasons.withId(seasonId),
        arrayOf(SeasonColumns.SEASON)
      )

      return if (c.moveToFirst()) c.getInt(0) else -1
    } finally {
      c?.close()
    }
  }

  fun getShowId(seasonId: Long): Long {
    var c: Cursor? = null
    try {
      c = context.contentResolver.query(
        Seasons.withId(seasonId),
        arrayOf(SeasonColumns.SHOW_ID)
      )

      return if (c.moveToFirst()) c.getLong(0) else -1L
    } finally {
      c?.close()
    }
  }

  class IdResult(var id: Long, var didCreate: Boolean)

  fun getIdOrCreate(showId: Long, season: Int): IdResult {
    Preconditions.checkArgument(showId >= 0, "showId must be >=0, was %d", showId)
    synchronized(LOCK_ID) {
      var id = getId(showId, season)

      if (id == -1L) {
        id = create(showId, season)
        return IdResult(id, true)
      } else {
        return IdResult(id, false)
      }
    }
  }

  private fun create(showId: Long, season: Int): Long {
    val values = ContentValues()
    values.put(SeasonColumns.SHOW_ID, showId)
    values.put(SeasonColumns.SEASON, season)

    val uri = context.contentResolver.insert(Seasons.SEASONS, values)
    return Seasons.getId(uri!!)
  }

  fun updateSeason(showId: Long, season: Season): Long {
    val result = getIdOrCreate(showId, season.number)
    val seasonId = result.id

    val values = getValues(season)
    context.contentResolver.update(Seasons.withId(seasonId), values, null, null)

    return seasonId
  }

  fun addToHistory(seasonId: Long, watchedAt: Long) {
    val values = ContentValues()
    values.put(EpisodeColumns.WATCHED, true)

    val firstAiredOffset = FirstAiredOffsetPreference.getInstance().offsetMillis
    val millis = System.currentTimeMillis() - firstAiredOffset

    context.contentResolver.update(
      Episodes.fromSeason(seasonId), values, EpisodeColumns.FIRST_AIRED + "<?",
      arrayOf(millis.toString())
    )

    if (watchedAt == WATCHED_RELEASE) {
      values.clear()

      val episodes = context.contentResolver.query(
        Episodes.fromSeason(seasonId),
        arrayOf(EpisodeColumns.ID, EpisodeColumns.FIRST_AIRED),
        EpisodeColumns.WATCHED + " AND " + EpisodeColumns.FIRST_AIRED + ">" + EpisodeColumns.LAST_WATCHED_AT
      )

      while (episodes.moveToNext()) {
        val episodeId = episodes.getLong(EpisodeColumns.ID)
        val firstAired = episodes.getLong(EpisodeColumns.FIRST_AIRED)
        values.put(EpisodeColumns.LAST_WATCHED_AT, firstAired)
        context.contentResolver.update(Episodes.withId(episodeId), values, null, null)
      }

      episodes.close()
    } else {
      values.clear()
      values.put(EpisodeColumns.LAST_WATCHED_AT, watchedAt)

      context.contentResolver.update(
        Episodes.fromSeason(seasonId),
        values,
        EpisodeColumns.WATCHED + " AND " + EpisodeColumns.LAST_WATCHED_AT + "<?",
        arrayOf(watchedAt.toString())
      )
    }
  }

  fun removeFromHistory(seasonId: Long) {
    val values = ContentValues()
    values.put(EpisodeColumns.WATCHED, false)
    values.put(EpisodeColumns.LAST_WATCHED_AT, 0L)
    context.contentResolver.update(Episodes.fromSeason(seasonId), values, null, null)
  }

  fun setIsInCollection(
    showTraktId: Long,
    seasonNumber: Int,
    collected: Boolean,
    collectedAt: Long
  ) {
    val showId = showHelper.getId(showTraktId)
    val seasonId = getId(showId, seasonNumber)
    setIsInCollection(seasonId, collected, collectedAt)
  }

  fun setIsInCollection(seasonId: Long, collected: Boolean, collectedAt: Long) {
    val episodes = context.contentResolver.query(
      Episodes.fromSeason(seasonId),
      arrayOf(EpisodeColumns.ID, EpisodeColumns.IN_COLLECTION)
    )

    val values = ContentValues()
    values.put(EpisodeColumns.IN_COLLECTION, collected)
    values.put(EpisodeColumns.COLLECTED_AT, collectedAt)

    while (episodes.moveToNext()) {
      val isCollected = episodes.getBoolean(EpisodeColumns.IN_COLLECTION)
      if (isCollected != collected) {
        val episodeId = episodes.getLong(EpisodeColumns.ID)
        context.contentResolver.update(Episodes.withId(episodeId), values)
      }
    }

    episodes.close()
  }

  private fun getValues(season: Season): ContentValues {
    val values = ContentValues()
    values.put(SeasonColumns.SEASON, season.number)
    values.put(SeasonColumns.FIRST_AIRED, season.first_aired?.timeInMillis)
    values.put(SeasonColumns.TVDB_ID, season.ids.tvdb)
    values.put(SeasonColumns.TMDB_ID, season.ids.tmdb)
    values.put(SeasonColumns.TVRAGE_ID, season.ids.tvrage)
    values.put(SeasonColumns.RATING, season.rating)
    values.put(SeasonColumns.VOTES, season.votes)
    return values
  }

  companion object {

    const val WATCHED_RELEASE = -1L

    private val LOCK_ID = Any()
  }
}
