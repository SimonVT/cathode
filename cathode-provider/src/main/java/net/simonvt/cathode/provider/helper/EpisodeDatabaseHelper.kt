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
import android.text.format.DateUtils
import net.simonvt.cathode.api.entity.Episode
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.provider.DatabaseContract
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.ProviderSchematic
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeDatabaseHelper @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper
) {

  fun getId(traktId: Long): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Episodes.EPISODES,
        arrayOf(EpisodeColumns.ID),
        EpisodeColumns.TRAKT_ID + "=?",
        arrayOf(traktId.toString())
      )
      val id = if (!c.moveToFirst()) -1L else c.getLong(EpisodeColumns.ID)
      c.close()
      return id
    }
  }

  fun getId(showId: Long, season: Int, episode: Int): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Episodes.EPISODES,
        arrayOf(EpisodeColumns.ID),
        EpisodeColumns.SHOW_ID + "=? AND " + EpisodeColumns.SEASON + "=? AND " + EpisodeColumns.EPISODE + "=?",
        arrayOf(showId.toString(), season.toString(), episode.toString())
      )
      val id = if (!c.moveToFirst()) -1L else c.getLong(EpisodeColumns.ID)
      c.close()
      return id
    }
  }

  fun getId(showId: Long, seasonId: Long, episode: Int): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Episodes.EPISODES,
        arrayOf(EpisodeColumns.ID),
        EpisodeColumns.SHOW_ID + "=? AND " + EpisodeColumns.SEASON_ID + "=? AND " + EpisodeColumns.EPISODE + "=?",
        arrayOf(showId.toString(), seasonId.toString(), episode.toString())
      )
      val id = if (!c.moveToFirst()) -1L else c.getLong(EpisodeColumns.ID)
      c.close()
      return id
    }
  }

  fun getTraktId(episodeId: Long): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Episodes.withId(episodeId),
        arrayOf(EpisodeColumns.TRAKT_ID)
      )
      val traktId = if (!c.moveToFirst()) -1L else c.getLong(EpisodeColumns.TRAKT_ID)
      c.close()
      return traktId
    }
  }

  fun getTmdbId(episodeId: Long): Int {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Episodes.withId(episodeId),
        arrayOf(EpisodeColumns.TMDB_ID)
      )
      val traktId = if (!c.moveToFirst()) -1 else c.getInt(EpisodeColumns.TMDB_ID)
      c.close()
      return traktId
    }
  }

  class IdResult(var id: Long, var didCreate: Boolean)

  fun getIdOrCreate(showId: Long, seasonId: Long, episode: Int): IdResult {
    Preconditions.checkArgument(showId >= 0, "showId must be >=0, was %d", showId)
    Preconditions.checkArgument(seasonId >= 0, "seasonId must be >=0, was %d", seasonId)
    synchronized(LOCK_ID) {
      var id = getId(showId, seasonId, episode)

      if (id == -1L) {
        id = create(showId, seasonId, episode)
        return IdResult(id, true)
      } else {
        return IdResult(id, false)
      }
    }
  }

  private fun create(showId: Long, seasonId: Long, episode: Int): Long {
    val season = seasonHelper.getNumber(seasonId)

    val values = ContentValues()
    values.put(EpisodeColumns.SHOW_ID, showId)
    values.put(EpisodeColumns.SEASON_ID, seasonId)
    values.put(EpisodeColumns.SEASON, season)
    values.put(EpisodeColumns.EPISODE, episode)

    return Episodes.getId(context.contentResolver.insert(Episodes.EPISODES, values)!!)
  }

  fun updateEpisode(episodeId: Long, episode: Episode): Long {
    val values = getValues(episode)
    context.contentResolver.update(Episodes.withId(episodeId), values, null, null)
    return episodeId
  }

  fun getShowId(episodeId: Long): Long {
    val c =
      context.contentResolver.query(Episodes.withId(episodeId), arrayOf(EpisodeColumns.SHOW_ID))
    val id = if (c.moveToFirst()) c.getLong(EpisodeColumns.SHOW_ID) else -1L
    c.close()
    return id
  }

  fun getSeason(episodeId: Long): Int {
    val c =
      context.contentResolver.query(Episodes.withId(episodeId), arrayOf(EpisodeColumns.SEASON))
    val season = if (c.moveToFirst()) c.getInt(EpisodeColumns.SEASON) else -1
    c.close()
    return season
  }

  fun getNumber(episodeId: Long): Int {
    val c =
      context.contentResolver.query(Episodes.withId(episodeId), arrayOf(EpisodeColumns.EPISODE))
    val number = if (!c.moveToFirst()) -1 else c.getInt(EpisodeColumns.EPISODE)
    c.close()
    return number
  }

  fun checkIn(episodeId: Long) {
    val currentTime = System.currentTimeMillis()
    val showId = getShowId(episodeId)

    val show = context.contentResolver.query(
      ProviderSchematic.Shows.withId(showId),
      arrayOf(DatabaseContract.ShowColumns.RUNTIME)
    )

    show.moveToFirst()
    val runtime = show.getInt(DatabaseContract.ShowColumns.RUNTIME)
    show.close()

    val expiresAt = currentTime + runtime * DateUtils.MINUTE_IN_MILLIS

    val values = ContentValues()
    values.put(EpisodeColumns.CHECKED_IN, true)
    values.put(EpisodeColumns.STARTED_AT, currentTime)
    values.put(EpisodeColumns.EXPIRES_AT, expiresAt)
    context.contentResolver.update(Episodes.withId(episodeId), values, null, null)
  }

  fun addToHistory(episodeId: Long, watchedAt: Long) {
    val values = ContentValues()
    values.put(EpisodeColumns.WATCHED, true)

    context.contentResolver.update(Episodes.withId(episodeId), values, null, null)

    if (watchedAt == WATCHED_RELEASE) {
      values.clear()
      val episode = context.contentResolver.query(
        Episodes.withId(episodeId),
        arrayOf(EpisodeColumns.FIRST_AIRED),
        EpisodeColumns.WATCHED + " AND " + EpisodeColumns.FIRST_AIRED + ">" + EpisodeColumns.LAST_WATCHED_AT
      )
      if (episode.moveToNext()) {
        val firstAired = episode.getLong(EpisodeColumns.FIRST_AIRED)
        values.put(EpisodeColumns.LAST_WATCHED_AT, firstAired)
        context.contentResolver.update(Episodes.withId(episodeId), values, null, null)
      }
      episode.close()
    } else {
      values.clear()
      values.put(EpisodeColumns.LAST_WATCHED_AT, watchedAt)

      context.contentResolver.update(
        Episodes.withId(episodeId),
        values,
        EpisodeColumns.WATCHED + " AND " + EpisodeColumns.LAST_WATCHED_AT + "<?",
        arrayOf(watchedAt.toString())
      )
    }
  }

  fun removeFromHistory(episodeId: Long) {
    val values = ContentValues()
    values.put(EpisodeColumns.WATCHED, false)
    values.put(EpisodeColumns.LAST_WATCHED_AT, 0L)
    context.contentResolver.update(Episodes.withId(episodeId), values, null, null)
  }

  fun setInCollection(episodeId: Long, inCollection: Boolean, collectedAt: Long) {
    val values = ContentValues()
    values.put(EpisodeColumns.IN_COLLECTION, inCollection)
    values.put(EpisodeColumns.COLLECTED_AT, collectedAt)

    context.contentResolver.update(Episodes.withId(episodeId), values, null, null)
  }

  fun setIsInWatchlist(
    traktId: Long,
    season: Int,
    episode: Int,
    inWatchlist: Boolean,
    listedAt: Long
  ) {
    val showId = showHelper.getId(traktId)
    val episodeId = getId(showId, season, episode)
    setIsInWatchlist(episodeId, inWatchlist, listedAt)
  }

  fun setIsInWatchlist(episodeId: Long, inWatchlist: Boolean, listedAt: Long) {
    val values = ContentValues()
    values.put(EpisodeColumns.IN_WATCHLIST, inWatchlist)
    values.put(EpisodeColumns.LISTED_AT, listedAt)

    context.contentResolver.update(Episodes.withId(episodeId), values, null, null)
  }

  fun getValues(episode: Episode): ContentValues {
    val values = ContentValues()
    values.put(EpisodeColumns.SEASON, episode.season)
    values.put(EpisodeColumns.EPISODE, episode.number)
    values.put(EpisodeColumns.NUMBER_ABS, episode.number_abs)
    values.put(EpisodeColumns.TITLE, episode.title)
    values.put(EpisodeColumns.OVERVIEW, episode.overview)
    values.put(EpisodeColumns.FIRST_AIRED, episode.first_aired?.timeInMillis)
    values.put(EpisodeColumns.UPDATED_AT, episode.updated_at?.timeInMillis)
    values.put(EpisodeColumns.TRAKT_ID, episode.ids.trakt)
    values.put(EpisodeColumns.IMDB_ID, episode.ids.imdb)
    values.put(EpisodeColumns.TVDB_ID, episode.ids.tvdb)
    values.put(EpisodeColumns.TMDB_ID, episode.ids.tmdb)
    values.put(EpisodeColumns.TVRAGE_ID, episode.ids.tvrage)
    values.put(EpisodeColumns.RATING, episode.rating)
    values.put(EpisodeColumns.VOTES, episode.votes)
    return values
  }

  companion object {

    const val WATCHED_RELEASE = -1L

    private val LOCK_ID = Any()
  }
}
