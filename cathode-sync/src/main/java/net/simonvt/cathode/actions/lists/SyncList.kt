/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.actions.lists

import android.content.ContentProviderOperation
import android.content.Context
import androidx.work.WorkManager
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.lists.SyncList.Params
import net.simonvt.cathode.actions.people.SyncPerson
import net.simonvt.cathode.api.entity.ListItem
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns
import net.simonvt.cathode.provider.ProviderSchematic.ListItems
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.ListWrapper
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import retrofit2.Call
import javax.inject.Inject

class SyncList @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val movieHelper: MovieDatabaseHelper,
  private val personHelper: PersonDatabaseHelper,
  private val usersService: UsersService,
  private val syncPerson: SyncPerson,
  private val workManager: WorkManager
) : CallAction<Params, List<ListItem>>() {

  private class Item constructor(var itemType: Int, var itemId: Long)

  private fun getItemPosition(items: List<Item>, itemType: Int, itemId: Long): Int {
    var i = 0
    val count = items.size
    while (i < count) {
      val item = items[i]
      if (item.itemType == itemType && item.itemId == itemId) {
        return i
      }
      i++
    }

    return -1
  }

  override fun key(params: Params): String = "SyncList&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<List<ListItem>> =
    usersService.listItems(params.traktId)

  override suspend fun handleResponse(params: Params, response: List<ListItem>) {
    val listId = ListWrapper.getId(context.contentResolver, params.traktId)
    if (listId == -1L) {
      // List has been removed
      return
    }

    val oldItems = mutableListOf<Item>()
    val localListItems = context.contentResolver.query(
      ListItems.inList(listId),
      arrayOf(ListItemColumns.ITEM_TYPE, ListItemColumns.ITEM_ID)
    )
    localListItems.forEach { cursor ->
      oldItems.add(
        Item(
          cursor.getInt(ListItemColumns.ITEM_TYPE),
          cursor.getLong(ListItemColumns.ITEM_ID)
        )
      )
    }
    localListItems.close()

    val ops = arrayListOf<ContentProviderOperation>()
    var syncPendingShows = false
    var syncPendingMovies = false

    for (listItem in response) {
      when (listItem.type) {
        ItemType.SHOW -> {
          val showTraktId = listItem.show!!.ids.trakt!!
          val showResult = showHelper.getIdOrCreate(showTraktId)
          val showId = showResult.showId
          val lastSync = showHelper.lastSync(showId)
          if (lastSync == 0L) {
            showHelper.markPending(showId)
            syncPendingShows = true
          }

          val itemPosition = getItemPosition(oldItems, DatabaseContract.ItemType.SHOW, showId)
          if (itemPosition >= 0) {
            oldItems.removeAt(itemPosition)
          } else {
            val opBuilder = ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
              .withValue(ListItemColumns.LISTED_AT, listItem.listed_at.timeInMillis)
              .withValue(ListItemColumns.LIST_ID, listId)
              .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.SHOW)
              .withValue(ListItemColumns.ITEM_ID, showId)
            ops.add(opBuilder.build())
          }
        }

        ItemType.SEASON -> {
          val show = listItem.show
          val showTraktId = show!!.ids.trakt!!
          val showResult = showHelper.getIdOrCreate(showTraktId)
          val showId = showResult.showId
          val lastSync = showHelper.lastSync(showId)

          val seasonNumber = listItem.season!!.number
          val seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber)
          val seasonId = seasonResult.id
          if (lastSync == 0L || seasonResult.didCreate) {
            showHelper.markPending(showId)
            syncPendingShows = true
          }

          val itemPosition = getItemPosition(oldItems, DatabaseContract.ItemType.SEASON, seasonId)
          if (itemPosition >= 0) {
            oldItems.removeAt(itemPosition)
          } else {
            val opBuilder = ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
              .withValue(ListItemColumns.LISTED_AT, listItem.listed_at.timeInMillis)
              .withValue(ListItemColumns.LIST_ID, listId)
              .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.SEASON)
              .withValue(ListItemColumns.ITEM_ID, seasonId)
            ops.add(opBuilder.build())
          }
        }

        ItemType.EPISODE -> {
          val show = listItem.show!!
          val episode = listItem.episode!!

          val showTraktId = show.ids.trakt!!
          val showResult = showHelper.getIdOrCreate(showTraktId)
          val showId = showResult.showId
          val lastSync = showHelper.lastSync(showId)

          val seasonNumber = episode.season!!
          val seasonResult = seasonHelper.getIdOrCreate(showId, seasonNumber)
          val seasonId = seasonResult.id

          val episodeResult =
            episodeHelper.getIdOrCreate(showId, seasonId, episode.number!!)
          val episodeId = episodeResult.id
          if (lastSync == 0L || episodeResult.didCreate) {
            showHelper.markPending(showId)
            syncPendingShows = true
          }

          val itemPosition = getItemPosition(oldItems, DatabaseContract.ItemType.EPISODE, episodeId)
          if (itemPosition >= 0) {
            oldItems.removeAt(itemPosition)
          } else {
            val opBuilder = ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
              .withValue(ListItemColumns.LISTED_AT, listItem.listed_at.timeInMillis)
              .withValue(ListItemColumns.LIST_ID, listId)
              .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.EPISODE)
              .withValue(ListItemColumns.ITEM_ID, episodeId)
            ops.add(opBuilder.build())
          }
        }

        ItemType.MOVIE -> {
          val movieTraktId = listItem.movie!!.ids.trakt!!
          val result = movieHelper.getIdOrCreate(movieTraktId)
          val movieId = result.movieId
          val lastSync = movieHelper.lastSync(movieId)
          if (lastSync == 0L) {
            movieHelper.markPending(movieId)
            syncPendingMovies = true
          }

          val itemPosition = getItemPosition(oldItems, DatabaseContract.ItemType.MOVIE, movieId)
          if (itemPosition >= 0) {
            oldItems.removeAt(itemPosition)
          } else {
            val opBuilder = ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
              .withValue(ListItemColumns.LISTED_AT, listItem.listed_at.timeInMillis)
              .withValue(ListItemColumns.LIST_ID, listId)
              .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.MOVIE)
              .withValue(ListItemColumns.ITEM_ID, movieId)
            ops.add(opBuilder.build())
          }
        }

        ItemType.PERSON -> {
          val traktId = listItem.person!!.ids.trakt!!
          var personId = personHelper.getId(traktId)
          if (personId == -1L) {
            personId = personHelper.partialUpdate(listItem.person)
            syncPerson.invokeSync(SyncPerson.Params(traktId))
          }

          val itemPosition = getItemPosition(oldItems, DatabaseContract.ItemType.PERSON, personId)
          if (itemPosition >= 0) {
            oldItems.removeAt(itemPosition)
          } else {
            val opBuilder = ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
              .withValue(ListItemColumns.LISTED_AT, listItem.listed_at.timeInMillis)
              .withValue(ListItemColumns.LIST_ID, listId)
              .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.PERSON)
              .withValue(ListItemColumns.ITEM_ID, personId)
            ops.add(opBuilder.build())
          }
        }
        else -> IllegalArgumentException("Unknown type: " + listItem.type)
      }
    }

    for (item in oldItems) {
      val opBuilder = ContentProviderOperation.newDelete(ListItems.LIST_ITEMS)
        .withSelection(
          ListItemColumns.ITEM_TYPE + "=? AND " + ListItemColumns.ITEM_ID + "=?",
          arrayOf(item.itemType.toString(), item.itemId.toString())
        )
      ops.add(opBuilder.build())
    }

    if (syncPendingShows) {
      workManager.enqueueUniqueNow(SyncPendingShowsWorker.TAG, SyncPendingShowsWorker::class.java)
    }
    if (syncPendingMovies) {
      workManager.enqueueUniqueNow(SyncPendingMoviesWorker.TAG, SyncPendingMoviesWorker::class.java)
    }

    context.contentResolver.batch(ops)
  }

  data class Params(val traktId: Long)
}
