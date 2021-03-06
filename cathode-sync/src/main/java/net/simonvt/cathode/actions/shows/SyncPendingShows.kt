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

package net.simonvt.cathode.actions.shows

import android.content.Context
import net.simonvt.cathode.actions.ActionFailedException
import net.simonvt.cathode.actions.ErrorHandlerAction
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.event.ItemsUpdatedEvent
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.ListItems
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.entity.ItemTypeString
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class SyncPendingShows @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val syncShow: SyncShow
) : ErrorHandlerAction<Unit>() {

  override fun key(params: Unit): String = "SyncPendingShows"

  override suspend fun invoke(params: Unit) {
    val syncItems = mutableMapOf<Long, Long>()

    val where = (ShowColumns.NEEDS_SYNC + "=1 AND (" +
        ShowColumns.WATCHED_COUNT + ">0 OR " +
        ShowColumns.IN_COLLECTION_COUNT + ">0 OR " +
        ShowColumns.IN_WATCHLIST_COUNT + ">0 OR " +
        ShowColumns.IN_WATCHLIST + "=1 OR " +
        ShowColumns.HIDDEN_CALENDAR + "=1 OR " +
        ShowColumns.HIDDEN_WATCHED + "=1 OR " +
        ShowColumns.HIDDEN_COLLECTED + "=1)")
    val userShows = context.contentResolver.query(
      Shows.SHOWS,
      arrayOf(ShowColumns.ID, ShowColumns.TRAKT_ID),
      where
    )
    userShows.forEach {
      val showId = userShows.getLong(ShowColumns.ID)
      val traktId = userShows.getLong(ShowColumns.TRAKT_ID)

      if (syncItems[showId] == null) {
        syncItems[showId] = traktId
      }
    }
    userShows.close()

    val listShows = context.contentResolver.query(
      ListItems.LIST_ITEMS,
      arrayOf(ListItemColumns.ITEM_ID),
      ListItemColumns.ITEM_TYPE + "=?",
      arrayOf(ItemTypeString.SHOW)
    )
    listShows.forEach {
      val showId = listShows.getLong(ListItemColumns.ITEM_ID)
      if (syncItems[showId] == null) {
        val traktId = showHelper.getTraktId(showId)
        val needsSync = showHelper.needsSync(showId)
        if (needsSync) {
          syncItems[showId] = traktId
        }
      }
    }
    listShows.close()

    val listSeasons = context.contentResolver.query(
      ListItems.LIST_ITEMS,
      arrayOf(ListItemColumns.ITEM_ID),
      ListItemColumns.ITEM_TYPE + "=?",
      arrayOf(ItemTypeString.SEASON)
    )
    listSeasons.forEach {
      val seasonId = listSeasons.getLong(ListItemColumns.ITEM_ID)
      val showId = seasonHelper.getShowId(seasonId)
      if (syncItems[showId] == null) {
        val traktId = showHelper.getTraktId(showId)
        val needsSync = showHelper.needsSync(showId)
        if (needsSync) {
          syncItems[showId] = traktId
        }
      }
    }
    listSeasons.close()

    val listEpisodes = context.contentResolver.query(
      ListItems.LIST_ITEMS,
      arrayOf(ListItemColumns.ITEM_ID),
      ListItemColumns.ITEM_TYPE + "=?",
      arrayOf(ItemTypeString.EPISODE)
    )
    listEpisodes.forEach {
      val episodeId = listEpisodes.getLong(ListItemColumns.ITEM_ID)
      val showId = episodeHelper.getShowId(episodeId)
      if (syncItems[showId] == null) {
        val traktId = showHelper.getTraktId(showId)
        val needsSync = showHelper.needsSync(showId)
        if (needsSync) {
          syncItems[showId] = traktId
        }
      }
    }
    listEpisodes.close()

    try {
      for ((showId, traktId) in syncItems) {
        Timber.d("Syncing pending show %d", traktId)
        syncShow.invokeSync(SyncShow.Params(traktId))

        if (stopped) {
          return
        }
      }

      ItemsUpdatedEvent.post()
    } catch (e: IOException) {
      throw ActionFailedException(e)
    }
  }
}
