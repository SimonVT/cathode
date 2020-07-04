/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.database.Cursor
import android.os.RemoteException
import android.text.format.DateUtils
import net.simonvt.cathode.api.entity.Movie
import net.simonvt.cathode.common.database.DatabaseUtils
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.common.util.TextUtils
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.generated.CathodeProvider
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.update
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieDatabaseHelper @Inject constructor(private val context: Context) {

  fun getTraktId(movieId: Long): Long {
    val c = context.contentResolver.query(Movies.withId(movieId), arrayOf(MovieColumns.TRAKT_ID))
    val traktId = if (c.moveToFirst()) c.getLong(MovieColumns.TRAKT_ID) else -1
    c.close()
    return traktId
  }

  fun getTmdbId(movieId: Long): Int {
    val c = context.contentResolver.query(Movies.withId(movieId), arrayOf(MovieColumns.TMDB_ID))
    val traktId = if (c.moveToFirst()) c.getInt(MovieColumns.TMDB_ID) else -1
    c.close()
    return traktId
  }

  fun getId(traktId: Long): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Movies.MOVIES,
        arrayOf(MovieColumns.ID),
        MovieColumns.TRAKT_ID + "=?",
        arrayOf(traktId.toString())
      )
      val id = if (!c.moveToFirst()) -1L else c.getLong(MovieColumns.ID)
      c.close()
      return id
    }
  }

  fun getIdFromTmdb(tmdbId: Int): Long {
    synchronized(LOCK_ID) {
      val c = context.contentResolver.query(
        Movies.MOVIES,
        arrayOf(MovieColumns.ID),
        MovieColumns.TMDB_ID + "=?",
        arrayOf(tmdbId.toString())
      )
      val id = if (!c.moveToFirst()) -1L else c.getLong(MovieColumns.ID)
      c.close()
      return id
    }
  }

  fun needsSync(movieId: Long): Boolean {
    var movie: Cursor? = null
    try {
      movie =
        context.contentResolver.query(Movies.withId(movieId), arrayOf(MovieColumns.NEEDS_SYNC))
      return if (movie.moveToFirst()) movie.getBoolean(MovieColumns.NEEDS_SYNC) else false
    } finally {
      movie?.close()
    }
  }

  fun lastSync(movieId: Long): Long {
    var movie: Cursor? = null
    try {
      movie = context.contentResolver.query(Movies.withId(movieId), arrayOf(MovieColumns.LAST_SYNC))
      return if (movie.moveToFirst()) movie.getLong(MovieColumns.LAST_SYNC) else 0L
    } finally {
      movie?.close()
    }
  }

  fun markPending(movieId: Long) {
    val values = ContentValues()
    values.put(MovieColumns.NEEDS_SYNC, true)
    context.contentResolver.update(Movies.withId(movieId), values, null, null)
  }

  fun isUpdated(traktId: Long, lastUpdated: Long): Boolean {
    var movie: Cursor? = null
    try {
      movie = context.contentResolver.query(
        Movies.MOVIES,
        arrayOf(MovieColumns.LAST_UPDATED),
        MovieColumns.TRAKT_ID + "=?",
        arrayOf(traktId.toString())
      )

      if (movie.moveToFirst()) {
        val movieLastUpdated = movie.getLong(MovieColumns.LAST_UPDATED)
        val currentTime = System.currentTimeMillis()
        if (movieLastUpdated > currentTime) {
          Timber.e(
            IllegalArgumentException("Last updated: $movieLastUpdated - current time: $currentTime"),
            "Wrong LAST_UPDATED"
          )
          return true
        }
        return lastUpdated > movieLastUpdated
      }

      return false
    } finally {
      movie?.close()
    }
  }

  class IdResult(var movieId: Long, var didCreate: Boolean)

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
    values.put(MovieColumns.TRAKT_ID, traktId)
    values.put(MovieColumns.NEEDS_SYNC, true)

    return Movies.getId(context.contentResolver.insert(Movies.MOVIES, values)!!)
  }

  fun fullUpdate(movie: Movie): Long {
    val result = getIdOrCreate(movie.ids.trakt!!)
    val id = result.movieId

    val values = getValues(movie)
    values.put(MovieColumns.NEEDS_SYNC, false)
    values.put(MovieColumns.LAST_SYNC, System.currentTimeMillis())
    context.contentResolver.update(Movies.withId(id), values)

    if (movie.genres != null) {
      insertGenres(id, movie.genres!!)
    }

    return id
  }

  fun partialUpdate(movie: Movie): Long {
    val result = getIdOrCreate(movie.ids.trakt!!)
    val id = result.movieId

    val values = getPartialValues(movie)
    context.contentResolver.update(Movies.withId(id), values)

    if (movie.genres != null) {
      insertGenres(id, movie.genres!!)
    }

    return id
  }

  private fun insertGenres(movieId: Long, genres: List<String>) {
    try {
      val ops = ArrayList<ContentProviderOperation>()
      var op: ContentProviderOperation

      op = ContentProviderOperation.newDelete(MovieGenres.fromMovie(movieId)).build()
      ops.add(op)

      for (genre in genres) {
        op = ContentProviderOperation.newInsert(MovieGenres.fromMovie(movieId))
          .withValue(MovieGenreColumns.MOVIE_ID, movieId)
          .withValue(MovieGenreColumns.GENRE, TextUtils.upperCaseFirstLetter(genre))
          .build()
        ops.add(op)
      }

      context.contentResolver.applyBatch(CathodeProvider.AUTHORITY, ops)
    } catch (e: RemoteException) {
      Timber.e(e, "Updating movie genres failed")
    } catch (e: OperationApplicationException) {
      Timber.e(e, "Updating movie genres failed")
    }
  }

  fun checkIn(movieId: Long) {
    val movie = context.contentResolver.query(
      Movies.withId(movieId),
      arrayOf(MovieColumns.RUNTIME)
    )

    movie.moveToFirst()
    val runtime = movie.getInt(MovieColumns.RUNTIME)
    movie.close()

    val startedAt = System.currentTimeMillis()
    val expiresAt = startedAt + runtime * DateUtils.MINUTE_IN_MILLIS

    val values = ContentValues()
    values.put(MovieColumns.CHECKED_IN, true)
    values.put(MovieColumns.STARTED_AT, startedAt)
    values.put(MovieColumns.EXPIRES_AT, expiresAt)
    context.contentResolver.update(Movies.withId(movieId), values, null, null)
  }

  fun addToHistory(movieId: Long, watchedAt: Long) {
    val values = ContentValues()
    values.put(MovieColumns.WATCHED, true)

    if (watchedAt == WATCHED_RELEASE) {
      val movie = context.contentResolver.query(
        Movies.withId(movieId),
        arrayOf(MovieColumns.RELEASED)
      )
      if (movie.moveToFirst()) {
        val released = movie.getString(MovieColumns.RELEASED)
        if (android.text.TextUtils.isEmpty(released)) {
          // No release date, just use current date. Trakt will mark it watched on release
          // and it will get synced back later.
          values.put(MovieColumns.WATCHED_AT, System.currentTimeMillis())
        } else {
          val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
          try {
            val releaseDate = df.parse(released).time
            values.put(MovieColumns.WATCHED_AT, releaseDate)
          } catch (e: ParseException) {
            Timber.e("Parsing release date %s failed", released)
            // Use current date.
            values.put(MovieColumns.WATCHED_AT, System.currentTimeMillis())
          }
        }
      }
      movie.close()
    } else {
      values.put(MovieColumns.WATCHED_AT, watchedAt)
    }

    context.contentResolver.update(Movies.withId(movieId), values, null, null)
  }

  fun removeFromHistory(movieId: Long) {
    val values = ContentValues()
    values.put(MovieColumns.WATCHED, false)
    values.put(MovieColumns.WATCHED_AT, 0L)
    context.contentResolver.update(Movies.withId(movieId), values, null, null)
  }

  @JvmOverloads
  fun setIsInCollection(movieId: Long, collected: Boolean, collectedAt: Long = 0) {
    val values = ContentValues()
    values.put(MovieColumns.IN_COLLECTION, collected)
    values.put(MovieColumns.COLLECTED_AT, collectedAt)
    context.contentResolver.update(Movies.withId(movieId), values, null, null)
  }

  @JvmOverloads
  fun setIsInWatchlist(movieId: Long, inWatchlist: Boolean, listedAt: Long = 0) {
    val values = ContentValues()
    values.put(MovieColumns.IN_WATCHLIST, inWatchlist)
    values.put(MovieColumns.LISTED_AT, listedAt)
    context.contentResolver.update(Movies.withId(movieId), values, null, null)
  }

  private fun getPartialValues(movie: Movie): ContentValues {
    val values = ContentValues()
    values.put(MovieColumns.TITLE, movie.title)
    values.put(MovieColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(movie.title))
    values.put(MovieColumns.TRAKT_ID, movie.ids.trakt)
    values.put(MovieColumns.SLUG, movie.ids.slug)
    values.put(MovieColumns.IMDB_ID, movie.ids.imdb)
    values.put(MovieColumns.TMDB_ID, movie.ids.tmdb)
    if (movie.overview != null) {
      values.put(MovieColumns.OVERVIEW, movie.overview)
    }
    if (movie.rating != null) {
      values.put(MovieColumns.RATING, movie.rating)
    }
    return values
  }

  private fun getValues(movie: Movie): ContentValues {
    val values = ContentValues()
    values.put(MovieColumns.TITLE, movie.title)
    values.put(MovieColumns.TITLE_NO_ARTICLE, DatabaseUtils.removeLeadingArticle(movie.title))
    values.put(MovieColumns.YEAR, movie.year)
    values.put(MovieColumns.RELEASED, movie.released)
    values.put(MovieColumns.RUNTIME, movie.runtime)
    values.put(MovieColumns.TAGLINE, movie.tagline)
    values.put(MovieColumns.OVERVIEW, movie.overview)
    values.put(MovieColumns.TRAKT_ID, movie.ids.trakt)
    values.put(MovieColumns.SLUG, movie.ids.slug)
    values.put(MovieColumns.IMDB_ID, movie.ids.imdb)
    values.put(MovieColumns.TMDB_ID, movie.ids.tmdb)
    values.put(MovieColumns.LANGUAGE, movie.language)
    values.put(MovieColumns.CERTIFICATION, movie.certification)
    values.put(MovieColumns.TRAILER, movie.trailer)
    values.put(MovieColumns.HOMEPAGE, movie.homepage)
    values.put(MovieColumns.RATING, movie.rating)
    values.put(MovieColumns.VOTES, movie.votes)
    return values
  }

  companion object {

    const val WATCHED_RELEASE = -1L

    private val LOCK_ID = Any()
  }
}
