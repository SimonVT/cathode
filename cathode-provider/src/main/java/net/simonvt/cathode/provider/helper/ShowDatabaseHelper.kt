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
import net.simonvt.cathode.api.entity.Show
import net.simonvt.cathode.common.database.DatabaseUtils
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.util.TextUtils
import net.simonvt.cathode.provider.DatabaseContract
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.update
import net.simonvt.cathode.settings.FirstAiredOffsetPreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowDatabaseHelper @Inject constructor(private val context: Context) {

  fun getId(traktId: Long): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Shows.SHOWS,
        arrayOf(ShowColumns.ID),
        ShowColumns.TRAKT_ID + "=?",
        arrayOf(traktId.toString())
      )

      val id = if (c.moveToFirst()) c.getLong(ShowColumns.ID) else -1L
      c.close()
      return id
    }
  }

  fun getIdFromTmdb(tmdbId: Int): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Shows.SHOWS,
        arrayOf(ShowColumns.ID),
        ShowColumns.TMDB_ID + "=?",
        arrayOf(tmdbId.toString())
      )

      val id = if (c.moveToFirst()) c.getLong(ShowColumns.ID) else -1L
      c.close()
      return id
    }
  }

  fun getTraktId(showId: Long): Long {
    val c = context.contentResolver.query(
      Shows.withId(showId),
      arrayOf(ShowColumns.TRAKT_ID)
    )

    val traktId = if (c.moveToFirst()) c.getLong(ShowColumns.TRAKT_ID) else -1L
    c.close()
    return traktId
  }

  fun getTmdbId(showId: Long): Int {
    val c = context.contentResolver.query(
      Shows.withId(showId),
      arrayOf(ShowColumns.TMDB_ID)
    )

    val tmdbId = if (c.moveToFirst()) c.getInt(ShowColumns.TMDB_ID) else -1
    c.close()
    return tmdbId
  }

  class IdResult(var showId: Long, var didCreate: Boolean)

  fun getIdOrCreate(traktId: Long): IdResult {
    synchronized(LOCK_ID) {
      var id = getId(traktId)

      if (id == -1L) {
        id = create(traktId)
        return IdResult(id, true)
      } else {
        return IdResult(id, false)
      }
    }
  }

  private fun create(traktId: Long): Long {
    val values = ContentValues()
    values.put(ShowColumns.TRAKT_ID, traktId)
    values.put(ShowColumns.NEEDS_SYNC, true)

    return Shows.getShowId(context.contentResolver.insert(Shows.SHOWS, values)!!)
  }

  fun fullUpdate(show: Show): Long {
    val result = getIdOrCreate(show.ids.trakt!!)
    val id = result.showId

    val values = getValues(show)
    values.put(ShowColumns.NEEDS_SYNC, false)
    values.put(ShowColumns.LAST_SYNC, System.currentTimeMillis())
    context.contentResolver.update(Shows.withId(id), values)

    if (show.genres != null) {
      insertShowGenres(id, show.genres!!)
    }

    return id
  }

  /**
   * Creates the show if it does not exist.
   */
  fun partialUpdate(show: Show): Long {
    val result = getIdOrCreate(show.ids.trakt!!)
    val id = result.showId

    val values = getPartialValues(show)
    context.contentResolver.update(Shows.withId(id), values)

    if (show.genres != null) {
      insertShowGenres(id, show.genres!!)
    }

    return id
  }

  fun getNextEpisodeId(showId: Long): Long {
    var lastWatchedSeason = -1
    var lastWatchedEpisode = -1

    val lastWatchedCursor = context.contentResolver.query(
      Episodes.fromShow(showId),
      arrayOf(EpisodeColumns.ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE),
      EpisodeColumns.WATCHED + "=1",
      null,
      EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1"
    )

    if (lastWatchedCursor!!.moveToFirst()) {
      lastWatchedSeason = lastWatchedCursor.getInt(EpisodeColumns.SEASON)
      lastWatchedEpisode = lastWatchedCursor.getInt(EpisodeColumns.EPISODE)
    }
    lastWatchedCursor.close()

    val nextEpisode = context.contentResolver.query(
      Episodes.fromShow(showId),
      arrayOf(EpisodeColumns.ID),
      EpisodeColumns.SEASON + ">0 AND (" +
          EpisodeColumns.SEASON + ">? OR (" +
          EpisodeColumns.SEASON + "=? AND " +
          EpisodeColumns.EPISODE + ">?)) AND " +
          EpisodeColumns.FIRST_AIRED + " NOT NULL",
      arrayOf(
        lastWatchedSeason.toString(),
        lastWatchedSeason.toString(),
        lastWatchedEpisode.toString()
      ),
      EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1"
    )

    var nextEpisodeId = -1L

    if (nextEpisode!!.moveToFirst()) {
      nextEpisodeId = nextEpisode.getLong(EpisodeColumns.ID)
    }

    nextEpisode.close()

    return nextEpisodeId
  }

  fun needsSync(showId: Long): Boolean {
    var show: Cursor? = null
    try {
      show = context.contentResolver.query(
        Shows.withId(showId),
        arrayOf(ShowColumns.NEEDS_SYNC)
      )

      return if (show.moveToFirst()) show.getBoolean(ShowColumns.NEEDS_SYNC) else false
    } finally {
      show?.close()
    }
  }

  fun lastSync(showId: Long): Long {
    var show: Cursor? = null
    try {
      show = context.contentResolver.query(
        Shows.withId(showId),
        arrayOf(ShowColumns.LAST_SYNC)
      )

      return if (show.moveToFirst()) show.getLong(ShowColumns.LAST_SYNC) else 0L
    } finally {
      show?.close()
    }
  }

  fun markPending(showId: Long) {
    val values = ContentValues()
    values.put(ShowColumns.NEEDS_SYNC, true)
    context.contentResolver.update(Shows.withId(showId), values, null, null)
  }

  fun isUpdated(traktId: Long, lastUpdated: Long): Boolean {
    var show: Cursor? = null
    try {
      show = context.contentResolver.query(
        Shows.SHOWS,
        arrayOf(ShowColumns.LAST_UPDATED),
        ShowColumns.TRAKT_ID + "=?",
        arrayOf(traktId.toString())
      )

      if (show.moveToFirst()) {
        val showLastUpdated = show.getLong(ShowColumns.LAST_UPDATED)
        return lastUpdated > showLastUpdated
      }

      return false
    } finally {
      show?.close()
    }
  }

  private fun insertShowGenres(showId: Long, genres: List<String>) {
    context.contentResolver.delete(ProviderSchematic.ShowGenres.fromShow(showId), null, null)

    for (genre in genres) {
      val values = ContentValues()

      values.put(DatabaseContract.ShowGenreColumns.SHOW_ID, showId)
      values.put(DatabaseContract.ShowGenreColumns.GENRE, TextUtils.upperCaseFirstLetter(genre))

      context.contentResolver.insert(ProviderSchematic.ShowGenres.fromShow(showId), values)
    }
  }

  fun addToHistory(showId: Long, watchedAt: Long) {
    val values = ContentValues()
    values.put(EpisodeColumns.WATCHED, true)

    val firstAiredOffset = FirstAiredOffsetPreference.getInstance().offsetMillis
    val millis = System.currentTimeMillis() - firstAiredOffset

    context.contentResolver.update(
      Episodes.fromShow(showId), values, EpisodeColumns.FIRST_AIRED + "<?",
      arrayOf(millis.toString())
    )

    if (watchedAt == WATCHED_RELEASE) {
      values.clear()

      val episodes = context.contentResolver.query(
        Episodes.fromShow(showId),
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
        Episodes.fromShow(showId),
        values,
        EpisodeColumns.WATCHED + " AND " + EpisodeColumns.LAST_WATCHED_AT + "<?",
        arrayOf(watchedAt.toString())
      )
    }
  }

  fun removeFromHistory(showId: Long) {
    val values = ContentValues()
    values.put(EpisodeColumns.WATCHED, false)
    values.put(EpisodeColumns.LAST_WATCHED_AT, 0)
    context.contentResolver.update(Episodes.fromShow(showId), null, null, null)
  }

  @JvmOverloads
  fun setIsInWatchlist(showId: Long, inWatchlist: Boolean, listedAt: Long = 0) {
    val values = ContentValues()
    values.put(ShowColumns.IN_WATCHLIST, inWatchlist)
    values.put(ShowColumns.LISTED_AT, listedAt)

    context.contentResolver.update(Shows.withId(showId), values, null, null)
  }

  fun setIsInCollection(traktId: Long, inCollection: Boolean) {
    val showId = getId(traktId)
    val values = ContentValues()
    values.put(EpisodeColumns.IN_COLLECTION, inCollection)

    val firstAiredOffset = FirstAiredOffsetPreference.getInstance().offsetMillis
    val millis = System.currentTimeMillis() - firstAiredOffset

    context.contentResolver.update(
      Episodes.fromShow(showId), values, EpisodeColumns.FIRST_AIRED + "<?",
      arrayOf(millis.toString())
    )
  }

  private fun getPartialValues(show: Show): ContentValues {
    val values = ContentValues()
    values.put(ShowColumns.TITLE, show.title)
    values.put(ShowColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(show.title))
    values.put(ShowColumns.TRAKT_ID, show.ids.trakt)
    values.put(ShowColumns.SLUG, show.ids.slug)
    values.put(ShowColumns.IMDB_ID, show.ids.imdb)
    values.put(ShowColumns.TVDB_ID, show.ids.tvdb)
    values.put(ShowColumns.TMDB_ID, show.ids.tmdb)
    values.put(ShowColumns.TVRAGE_ID, show.ids.tvrage)
    return values
  }

  private fun getValues(show: Show): ContentValues {
    val values = ContentValues()
    values.put(ShowColumns.TITLE, show.title)
    values.put(ShowColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(show.title))
    values.put(ShowColumns.YEAR, show.year)
    values.put(ShowColumns.FIRST_AIRED, show.first_aired?.timeInMillis)
    values.put(ShowColumns.COUNTRY, show.country)
    values.put(ShowColumns.OVERVIEW, show.overview)
    values.put(ShowColumns.RUNTIME, show.runtime)
    values.put(ShowColumns.NETWORK, show.network)
    values.put(ShowColumns.AIR_DAY, show.airs?.day)
    values.put(ShowColumns.AIR_TIME, show.airs?.time)
    values.put(ShowColumns.AIR_TIMEZONE, show.airs?.timezone)
    values.put(ShowColumns.CERTIFICATION, show.certification)
    values.put(ShowColumns.TRAILER, show.trailer)
    values.put(ShowColumns.HOMEPAGE, show.homepage)
    values.put(ShowColumns.STATUS, show.status?.toString())
    values.put(ShowColumns.TRAKT_ID, show.ids.trakt)
    values.put(ShowColumns.SLUG, show.ids.slug)
    values.put(ShowColumns.IMDB_ID, show.ids.imdb)
    values.put(ShowColumns.TVDB_ID, show.ids.tvdb)
    values.put(ShowColumns.TMDB_ID, show.ids.tmdb)
    values.put(ShowColumns.TVRAGE_ID, show.ids.tvrage)
    values.put(ShowColumns.LAST_UPDATED, show.updated_at?.timeInMillis)
    values.put(ShowColumns.RATING, show.rating)
    values.put(ShowColumns.VOTES, show.votes)
    return values
  }

  companion object {

    const val WATCHED_RELEASE = -1L

    private val LOCK_ID = Any()
  }
}
