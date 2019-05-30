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

package net.simonvt.cathode.sync.scheduler

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.launch
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.user.SyncLists
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.enumeration.Privacy
import net.simonvt.cathode.api.enumeration.SortBy
import net.simonvt.cathode.api.enumeration.SortOrientation
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns
import net.simonvt.cathode.provider.ProviderSchematic.ListItems
import net.simonvt.cathode.provider.ProviderSchematic.Lists
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper
import net.simonvt.cathode.provider.helper.ListDatabaseHelper
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.remote.action.lists.AddEpisode
import net.simonvt.cathode.remote.action.lists.AddMovie
import net.simonvt.cathode.remote.action.lists.AddPerson
import net.simonvt.cathode.remote.action.lists.AddSeason
import net.simonvt.cathode.remote.action.lists.AddShow
import net.simonvt.cathode.remote.action.lists.RemoveEpisode
import net.simonvt.cathode.remote.action.lists.RemoveMovie
import net.simonvt.cathode.remote.action.lists.RemovePerson
import net.simonvt.cathode.remote.action.lists.RemoveSeason
import net.simonvt.cathode.remote.action.lists.RemoveShow
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.sync.trakt.UserList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListsTaskScheduler @Inject constructor(
  context: Context,
  jobManager: JobManager,
  private val showHelper: ShowDatabaseHelper,
  private val seasonHelper: SeasonDatabaseHelper,
  private val episodeHelper: EpisodeDatabaseHelper,
  private val movieHelper: MovieDatabaseHelper,
  private val personHelper: PersonDatabaseHelper,
  private val listHelper: ListDatabaseHelper,
  private val userList: UserList,
  private val syncLists: SyncLists
) : BaseTaskScheduler(context, jobManager) {

  fun createList(
    name: String,
    description: String,
    privacy: Privacy,
    displayNumbers: Boolean,
    allowComments: Boolean,
    sortBy: SortBy,
    sortOrientation: SortOrientation
  ) {
    scope.launch {
      if (TraktLinkSettings.isLinked(context)) {
        userList.create(
          name,
          description,
          privacy,
          displayNumbers,
          allowComments,
          sortBy,
          sortOrientation
        )
        syncLists.invokeAsync(SyncLists.Params())
      } else {
        val values = ContentValues()
        values.put(ListsColumns.NAME, name)
        values.put(ListsColumns.DESCRIPTION, description)
        values.put(ListsColumns.PRIVACY, Privacy.PRIVATE.toString())
        values.put(ListsColumns.DISPLAY_NUMBERS, false)
        values.put(ListsColumns.ALLOW_COMMENTS, true)
        values.put(ListsColumns.SORT_BY, sortBy.toString())
        values.put(ListsColumns.SORT_ORIENTATION, sortOrientation.toString())
        context.contentResolver.insert(Lists.LISTS, values)
      }
    }
  }

  fun updateList(
    listId: Long,
    name: String,
    description: String,
    privacy: Privacy,
    displayNumbers: Boolean,
    allowComments: Boolean,
    sortBy: SortBy,
    sortOrientation: SortOrientation
  ) {
    scope.launch {
      if (TraktLinkSettings.isLinked(context)) {
        val traktId = listHelper.getTraktId(listId)
        userList.update(
          traktId,
          name,
          description,
          privacy,
          displayNumbers,
          allowComments,
          sortBy,
          sortOrientation
        )
        syncLists.invokeAsync(SyncLists.Params())
      } else {
        val values = ContentValues()
        values.put(ListsColumns.NAME, name)
        values.put(ListsColumns.DESCRIPTION, description)
        values.put(ListsColumns.PRIVACY, Privacy.PRIVATE.toString())
        values.put(ListsColumns.DISPLAY_NUMBERS, false)
        values.put(ListsColumns.ALLOW_COMMENTS, true)
        values.put(ListsColumns.SORT_BY, sortBy.toString())
        values.put(ListsColumns.SORT_ORIENTATION, sortOrientation.toString())
        context.contentResolver.update(Lists.withId(listId), values, null, null)
      }
    }
  }

  fun deleteList(listId: Long) {
    scope.launch {
      val list = context.contentResolver.query(
        Lists.withId(listId),
        arrayOf(ListsColumns.NAME, ListsColumns.TRAKT_ID)
      )
      if (list.moveToFirst()) {
        val name = list.getString(ListsColumns.NAME)

        if (TraktLinkSettings.isLinked(context)) {
          val traktId = list.getLong(ListsColumns.TRAKT_ID)

          if (userList.delete(traktId, name)) {
            context.contentResolver.delete(ListItems.inList(listId), null, null)
            context.contentResolver.delete(Lists.withId(listId), null, null)
          }

          syncLists.invokeAsync(SyncLists.Params())
        } else {
          context.contentResolver.delete(ListItems.inList(listId), null, null)
          context.contentResolver.delete(Lists.withId(listId), null, null)
        }
      }
      list.close()
    }
  }

  fun addItem(listId: Long, itemType: ItemType, itemId: Long) {
    updateListItem(listId, itemType, itemId, true)
  }

  fun removeItem(listId: Long, itemType: ItemType, itemId: Long) {
    updateListItem(listId, itemType, itemId, false)
  }

  private fun updateListItem(listId: Long, itemType: ItemType, itemId: Long, add: Boolean) {
    scope.launch {
      val listTraktId = listHelper.getTraktId(listId)
      if (add) {
        val values = ContentValues()
        values.put(ListItemColumns.LIST_ID, listId)
        values.put(ListItemColumns.LISTED_AT, System.currentTimeMillis())
        values.put(ListItemColumns.ITEM_ID, itemId)
        values.put(ListItemColumns.ITEM_TYPE, itemType.toString())

        context.contentResolver.insert(ListItems.LIST_ITEMS, values)
      } else {
        context.contentResolver
          .delete(
            ListItems.inList(listId),
            ListItemColumns.ITEM_TYPE + "=? AND " + ListItemColumns.ITEM_ID + "=?",
            arrayOf(itemType.toString(), itemId.toString())
          )
      }

      if (TraktLinkSettings.isLinked(context)) {
        when (itemType) {
          ItemType.SHOW -> {
            val showTraktId = showHelper.getTraktId(itemId)

            if (add) {
              queue(AddShow(listTraktId, showTraktId))
            } else {
              queue(RemoveShow(listTraktId, showTraktId))
            }
          }

          ItemType.SEASON -> {
            val showId = seasonHelper.getShowId(itemId)
            val showTraktId = showHelper.getTraktId(showId)
            val seasonNumber = seasonHelper.getNumber(itemId)

            if (add) {
              queue(AddSeason(listTraktId, showTraktId, seasonNumber))
            } else {
              queue(RemoveSeason(listTraktId, showTraktId, seasonNumber))
            }
          }

          ItemType.EPISODE -> {
            val showId = episodeHelper.getShowId(itemId)
            val showTraktId = showHelper.getTraktId(showId)
            val seasonNumber = episodeHelper.getSeason(itemId)
            val episodeNumber = episodeHelper.getNumber(itemId)

            if (add) {
              queue(AddEpisode(listTraktId, showTraktId, seasonNumber, episodeNumber))
            } else {
              queue(RemoveEpisode(listTraktId, showTraktId, seasonNumber, episodeNumber))
            }
          }

          ItemType.MOVIE -> {
            val movieTraktId = movieHelper.getTraktId(itemId)

            if (add) {
              queue(AddMovie(listTraktId, movieTraktId))
            } else {
              queue(RemoveMovie(listTraktId, movieTraktId))
            }
          }

          ItemType.PERSON -> {
            val personTraktId = personHelper.getTraktId(itemId)

            if (add) {
              queue(AddPerson(listTraktId, personTraktId))
            } else {
              queue(RemovePerson(listTraktId, personTraktId))
            }
          }

          else -> throw RuntimeException("Unknown item type: $itemType")
        }
      }
    }
  }
}
