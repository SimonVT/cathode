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

package net.simonvt.cathode.actions.movies

import android.content.Context
import net.simonvt.cathode.actions.ErrorHandlerAction
import net.simonvt.cathode.actions.movies.SyncMovie.Params
import net.simonvt.cathode.api.service.MoviesService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.event.ItemsUpdatedEvent
import net.simonvt.cathode.provider.DatabaseContract.ItemType
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.ListItems
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.query
import timber.log.Timber
import javax.inject.Inject

class SyncPendingMovies @Inject constructor(
  private val context: Context,
  private val syncMovie: SyncMovie,
  val moviesService: MoviesService,
  val movieHelper: MovieDatabaseHelper
) : ErrorHandlerAction<Unit>() {

  override suspend fun invoke(params: Unit) {
    val syncItems = mutableMapOf<Long, Long>()

    val where = (MovieColumns.NEEDS_SYNC + "=1 AND (" +
        MovieColumns.WATCHED + "=1 OR " +
        MovieColumns.IN_COLLECTION + "=1 OR " +
        MovieColumns.IN_WATCHLIST + "=1 OR " +
        MovieColumns.HIDDEN_CALENDAR + "=1)")
    val userMovies = context.contentResolver.query(
      Movies.MOVIES,
      arrayOf(MovieColumns.ID, MovieColumns.TRAKT_ID),
      where
    )
    userMovies.forEach { cursor ->
      val movieId = cursor.getLong(MovieColumns.ID)
      val traktId = cursor.getLong(MovieColumns.TRAKT_ID)

      if (syncItems[movieId] == null) {
        syncItems[movieId] = traktId
      }
    }
    userMovies.close()

    val listMovies = context.contentResolver.query(
      ListItems.LIST_ITEMS,
      arrayOf(ListItemColumns.ITEM_ID),
      ListItemColumns.ITEM_TYPE + "=" + ItemType.MOVIE
    )
    listMovies.forEach { cursor ->
      val movieId = cursor.getLong(ListItemColumns.ITEM_ID)
      if (syncItems[movieId] == null) {
        val needsSync = movieHelper.needsSync(movieId)
        if (needsSync) {
          val traktId = movieHelper.getTraktId(movieId)
          syncItems[movieId] = traktId
        }
      }
    }
    listMovies.close()

    syncItems.forEach { (_, traktId) ->
      Timber.d("Syncing pending movie %d", traktId)
      syncMovie(Params(traktId))

      if (stopped) {
        return
      }
    }

    ItemsUpdatedEvent.post()
  }
}
