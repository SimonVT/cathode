/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
package net.simonvt.cathode.actions.user

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.ActionFailedException
import net.simonvt.cathode.actions.OptionalBodyCallAction
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.movies.SyncMovie
import net.simonvt.cathode.actions.shows.SyncShow
import net.simonvt.cathode.api.entity.Watching
import net.simonvt.cathode.api.enumeration.Action
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import javax.inject.Inject

class SyncWatching @Inject constructor(
  private val context: Context,
  private val usersService: UsersService,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val movieHelper: MovieDatabaseHelper,
  private val syncShow: SyncShow,
  private val syncMovie: SyncMovie
) : OptionalBodyCallAction<Unit, Watching>() {

  override fun key(params: Unit): String = "SyncWatching"

  override fun getCall(params: Unit): Call<Watching> = usersService.watching()

  override suspend fun handleResponse(params: Unit, response: Watching?) {
    val ops = arrayListOf<ContentProviderOperation>()
    val episodeWatching = mutableListOf<Long>()

    val episodeWatchingCursor = context.contentResolver.query(
      Episodes.EPISODE_WATCHING,
      arrayOf(Tables.EPISODES + "." + EpisodeColumns.ID)
    )
    episodeWatchingCursor.forEach { cursor -> episodeWatching.add(cursor.getLong(EpisodeColumns.ID)) }
    episodeWatchingCursor.close()

    val movieWatching = mutableListOf<Long>()
    val movieWatchingCursor =
      context.contentResolver.query(Movies.WATCHING, arrayOf(MovieColumns.ID))
    movieWatchingCursor.forEach { cursor -> movieWatching.add(cursor.getLong(MovieColumns.ID)) }
    movieWatchingCursor.close()

    var op: ContentProviderOperation

    if (response != null) {
      when (response.type) {
        ItemType.EPISODE -> {
          val showTraktId = response.show!!.ids.trakt!!
          val showResult = showHelper.getIdOrCreate(showTraktId)
          val showId = showResult.showId

          var needsSync = showHelper.needsSync(showId)

          val seasonNumber = response.episode!!.season!!
          val seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber)
          val seasonId = seasonResult.id
          needsSync = needsSync && seasonResult.didCreate

          val episodeNumber = response.episode!!.number!!

          val episodeResult = episodeHelper.getIdOrCreate(showId, seasonId, episodeNumber)
          val episodeId = episodeResult.id
          needsSync = needsSync && episodeResult.didCreate

          episodeWatching.remove(episodeId)

          if (response.action == Action.CHECKIN) {
            op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
              .withValue(EpisodeColumns.CHECKED_IN, true)
              .withValue(EpisodeColumns.WATCHING, false)
              .withValue(EpisodeColumns.STARTED_AT, response.started_at.timeInMillis)
              .withValue(EpisodeColumns.EXPIRES_AT, response.expires_at.timeInMillis)
              .build()
          } else {
            op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
              .withValue(EpisodeColumns.CHECKED_IN, false)
              .withValue(EpisodeColumns.WATCHING, true)
              .withValue(EpisodeColumns.STARTED_AT, response.started_at.timeInMillis)
              .withValue(EpisodeColumns.EXPIRES_AT, response.expires_at.timeInMillis)
              .build()
          }
          ops.add(op)

          if (needsSync) {
            syncShow.invokeSync(SyncShow.Params(showTraktId))
          }
        }

        ItemType.MOVIE -> {
          val movieTraktId = response.movie!!.ids.trakt!!
          val result = movieHelper.getIdOrCreate(movieTraktId)
          val movieId = result.movieId

          if (movieHelper.needsSync(movieId)) {
            syncMovie.invokeSync(SyncMovie.Params(movieTraktId))
          }

          movieWatching.remove(movieId)

          if (response.action == Action.CHECKIN) {
            op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
              .withValue(MovieColumns.CHECKED_IN, true)
              .withValue(MovieColumns.WATCHING, false)
              .withValue(MovieColumns.STARTED_AT, response.started_at.timeInMillis)
              .withValue(MovieColumns.EXPIRES_AT, response.expires_at.timeInMillis)
              .build()
          } else {
            op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
              .withValue(MovieColumns.CHECKED_IN, false)
              .withValue(MovieColumns.WATCHING, true)
              .withValue(MovieColumns.STARTED_AT, response.started_at.timeInMillis)
              .withValue(MovieColumns.EXPIRES_AT, response.expires_at.timeInMillis)
              .build()
          }
          ops.add(op)
        }
        else -> throw ActionFailedException("Unknown item type " + response.type.toString())
      }
    }

    for (episodeId in episodeWatching) {
      op = ContentProviderOperation.newUpdate(Episodes.withId(episodeId))
        .withValue(EpisodeColumns.CHECKED_IN, false)
        .withValue(EpisodeColumns.WATCHING, false)
        .build()
      ops.add(op)
    }

    for (movieId in movieWatching) {
      op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
        .withValue(MovieColumns.CHECKED_IN, false)
        .withValue(MovieColumns.WATCHING, false)
        .build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)
  }
}
