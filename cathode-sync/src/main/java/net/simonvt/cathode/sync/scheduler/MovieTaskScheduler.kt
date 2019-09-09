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
package net.simonvt.cathode.sync.scheduler

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.text.format.DateUtils
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import net.simonvt.cathode.api.body.SyncItems
import net.simonvt.cathode.api.service.CheckinService
import net.simonvt.cathode.api.util.TimeUtils
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getStringOrNull
import net.simonvt.cathode.common.event.ErrorEvent
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.remote.action.RemoveHistoryItem
import net.simonvt.cathode.remote.action.movies.AddMovieToHistory
import net.simonvt.cathode.remote.action.movies.CalendarHideMovie
import net.simonvt.cathode.remote.action.movies.CollectMovie
import net.simonvt.cathode.remote.action.movies.DismissMovieRecommendation
import net.simonvt.cathode.remote.action.movies.RateMovie
import net.simonvt.cathode.remote.action.movies.RemoveMovieFromHistory
import net.simonvt.cathode.remote.action.movies.WatchlistMovie
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.R
import net.simonvt.cathode.sync.trakt.CheckIn
import net.simonvt.cathode.work.enqueueNow
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import net.simonvt.cathode.work.user.SyncWatchedMoviesWorker
import net.simonvt.cathode.work.user.SyncWatchingWorker
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieTaskScheduler @Inject
constructor(
  context: Context,
  jobManager: JobManager,
  private val workManager: WorkManager,
  private val movieHelper: MovieDatabaseHelper,
  private val checkinService: CheckinService,
  private val checkIn: CheckIn
) : BaseTaskScheduler(context, jobManager) {

  fun addToHistoryNow(movieId: Long) {
    addToHistory(movieId, System.currentTimeMillis())
  }

  fun addToHistoryOnRelease(movieId: Long) {
    addToHistory(movieId, SyncItems.TIME_RELEASED)
  }

  fun addToHistory(movieId: Long, watchedAt: Long) {
    val isoWhen = TimeUtils.getIsoTime(watchedAt)
    addToHistory(movieId, isoWhen)
  }

  fun addToHistory(
    movieId: Long,
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int
  ) {
    addToHistory(movieId, TimeUtils.getMillis(year, month, day, hour, minute))
  }

  /**
   * Add episodes watched outside of trakt to user library.
   *
   * @param movieId The database id of the episode.
   */
  fun addToHistory(movieId: Long, watchedAt: String) {
    scope.launch {
      if (SyncItems.TIME_RELEASED == watchedAt) {
        movieHelper.addToHistory(movieId, MovieDatabaseHelper.WATCHED_RELEASE)
      } else {
        movieHelper.addToHistory(movieId, TimeUtils.getMillis(watchedAt))
      }

      if (movieHelper.lastSync(movieId) == 0L) {
        workManager.enqueueUniqueNow(
          SyncPendingMoviesWorker.TAG,
          SyncPendingMoviesWorker::class.java
        )
      }

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = movieHelper.getTraktId(movieId)
        queue(AddMovieToHistory(traktId, watchedAt))
      }
    }
  }

  fun removeFromHistory(movieId: Long) {
    scope.launch {
      movieHelper.removeFromHistory(movieId)

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = movieHelper.getTraktId(movieId)
        queue(RemoveMovieFromHistory(traktId))
      }
    }
  }

  fun removeHistoryItem(movieId: Long, historyId: Long, lastItem: Boolean) {
    scope.launch {
      if (lastItem) {
        movieHelper.removeFromHistory(movieId)
      }

      if (TraktLinkSettings.isLinked(context)) {
        queue(RemoveHistoryItem(historyId))
        workManager.enqueueNow(SyncWatchedMoviesWorker::class.java)
      }
    }
  }

  fun setIsInWatchlist(movieId: Long, inWatchlist: Boolean) {
    scope.launch {
      var listedAt: String? = null
      var listedAtMillis = 0L
      if (inWatchlist) {
        listedAt = TimeUtils.getIsoTime()
        listedAtMillis = TimeUtils.getMillis(listedAt)
      }

      movieHelper.setIsInWatchlist(movieId, inWatchlist, listedAtMillis)

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = movieHelper.getTraktId(movieId)
        queue(WatchlistMovie(traktId, inWatchlist, listedAt))
      }
    }
  }

  fun setIsInCollection(movieId: Long, inCollection: Boolean) {
    scope.launch {
      var collectedAt: String? = null
      var collectedAtMillis = 0L
      if (inCollection) {
        collectedAt = TimeUtils.getIsoTime()
        collectedAtMillis = TimeUtils.getMillis(collectedAt)
      }

      movieHelper.setIsInCollection(movieId, inCollection, collectedAtMillis)

      if (movieHelper.lastSync(movieId) == 0L) {
        workManager.enqueueUniqueNow(
          SyncPendingMoviesWorker.TAG,
          SyncPendingMoviesWorker::class.java
        )
      }

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = movieHelper.getTraktId(movieId)
        queue(CollectMovie(traktId, inCollection, collectedAt))
      }
    }
  }

  /**
   * Check into a movie on trakt. Think of this method as in between a seen and a scrobble.
   * After checking in, the trakt will automatically display it as watching then switch over to
   * watched status once
   * the duration has elapsed.
   *
   * @param movieId The database id of the movie.
   */
  fun checkin(
    movieId: Long,
    message: String?,
    facebook: Boolean,
    twitter: Boolean,
    tumblr: Boolean
  ) {
    scope.launch {
      val watching = context.contentResolver.query(
        Movies.WATCHING,
        arrayOf(MovieColumns.RUNTIME, MovieColumns.EXPIRES_AT),
        null,
        null,
        null
      )

      if (watching!!.moveToFirst()) {
        val currentTime = System.currentTimeMillis()
        val runtime = watching.getInt(MovieColumns.RUNTIME)
        val expires = watching.getLong(MovieColumns.EXPIRES_AT)
        val watchSlop = (runtime.toFloat() * DateUtils.MINUTE_IN_MILLIS.toFloat() * 0.8f).toLong()

        val movie = context.contentResolver.query(
          Movies.withId(movieId),
          arrayOf(MovieColumns.TITLE)
        )
        movie.moveToFirst()
        val title = movie.getStringOrNull(MovieColumns.TITLE)
        movie.close()

        if (expires - watchSlop < currentTime && expires > 0) {
          if (checkIn.movie(movieId, message, facebook, twitter, tumblr)) {
            movieHelper.checkIn(movieId)
          }
        } else {
          ErrorEvent.post(context.getString(R.string.checkin_error_watching, title))
        }
      } else {
        if (checkIn.movie(movieId, message, facebook, twitter, tumblr)) {
          movieHelper.checkIn(movieId)
        }
      }

      watching.close()

      workManager.enqueueNow(SyncWatchingWorker::class.java)
    }
  }

  /**
   * Notify trakt that user wants to cancel their current check in.
   */
  fun cancelCheckin() {
    scope.launch {
      var movie: Cursor? = null
      try {
        movie = context.contentResolver.query(
          Movies.WATCHING,
          arrayOf(MovieColumns.ID, MovieColumns.STARTED_AT, MovieColumns.EXPIRES_AT)
        )

        if (movie.moveToFirst()) {
          val id = movie.getLong(MovieColumns.ID)
          val startedAt = movie.getLong(MovieColumns.STARTED_AT)
          val expiresAt = movie.getLong(MovieColumns.EXPIRES_AT)

          val values = ContentValues()
          values.put(MovieColumns.CHECKED_IN, false)
          context.contentResolver.update(Movies.WATCHING, values, null, null)

          try {
            val call = checkinService.deleteCheckin()
            val response = call.execute()
            if (response.isSuccessful) {
              workManager.enqueueNow(SyncWatchedMoviesWorker::class.java)
              return@launch
            }
          } catch (e: IOException) {
            Timber.d(e)
          }

          ErrorEvent.post(context.getString(R.string.checkin_cancel_error))

          values.clear()
          values.put(MovieColumns.CHECKED_IN, true)
          values.put(MovieColumns.STARTED_AT, startedAt)
          values.put(MovieColumns.EXPIRES_AT, expiresAt)
          context.contentResolver.update(Movies.withId(id), values, null, null)

          workManager.enqueueNow(SyncWatchedMoviesWorker::class.java)
          workManager.enqueueNow(SyncWatchingWorker::class.java)
        }
      } finally {
        movie?.close()
      }
    }
  }

  fun dismissRecommendation(movieId: Long) {
    scope.launch {
      val traktId = movieHelper.getTraktId(movieId)
      val values = ContentValues()
      values.put(MovieColumns.RECOMMENDATION_INDEX, -1)
      context.contentResolver.update(Movies.withId(movieId), values, null, null)
      queue(DismissMovieRecommendation(traktId))
    }
  }

  /**
   * Rate a movie on trakt. Depending on the user settings, this will also send out social updates
   * to facebook,
   * twitter, and tumblr.
   *
   * @param movieId The database id of the movie.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  fun rate(movieId: Long, rating: Int) {
    scope.launch {
      val ratedAt = TimeUtils.getIsoTime()
      val ratedAtMillis = TimeUtils.getMillis(ratedAt)

      val values = ContentValues()
      values.put(MovieColumns.USER_RATING, rating)
      values.put(MovieColumns.RATED_AT, ratedAtMillis)
      context.contentResolver.update(Movies.withId(movieId), values, null, null)

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = movieHelper.getTraktId(movieId)
        queue(RateMovie(traktId, rating, ratedAt))
      }
    }
  }

  fun hideFromCalendar(movieId: Long, hidden: Boolean) {
    scope.launch {
      val values = ContentValues()
      values.put(MovieColumns.HIDDEN_CALENDAR, hidden)
      context.contentResolver.update(Movies.withId(movieId), values, null, null)

      if (TraktLinkSettings.isLinked(context)) {
        val traktId = movieHelper.getTraktId(movieId)
        queue(CalendarHideMovie(traktId, hidden))
      }
    }
  }
}
