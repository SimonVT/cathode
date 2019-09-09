/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.lists

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.lists.SyncList
import net.simonvt.cathode.api.enumeration.ItemType.EPISODE
import net.simonvt.cathode.api.enumeration.ItemType.MOVIE
import net.simonvt.cathode.api.enumeration.ItemType.PERSON
import net.simonvt.cathode.api.enumeration.ItemType.SEASON
import net.simonvt.cathode.api.enumeration.ItemType.SHOW
import net.simonvt.cathode.api.enumeration.SortBy
import net.simonvt.cathode.api.enumeration.SortBy.ADDED
import net.simonvt.cathode.api.enumeration.SortBy.MY_RATING
import net.simonvt.cathode.api.enumeration.SortBy.PERCENTAGE
import net.simonvt.cathode.api.enumeration.SortBy.POPULARITY
import net.simonvt.cathode.api.enumeration.SortBy.RANDOM
import net.simonvt.cathode.api.enumeration.SortBy.RANK
import net.simonvt.cathode.api.enumeration.SortBy.RELEASED
import net.simonvt.cathode.api.enumeration.SortBy.RUNTIME
import net.simonvt.cathode.api.enumeration.SortBy.TITLE
import net.simonvt.cathode.api.enumeration.SortBy.VOTES
import net.simonvt.cathode.api.enumeration.SortOrientation
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.ListItem
import net.simonvt.cathode.entity.UserList
import net.simonvt.cathode.entitymapper.ListItemListMapper
import net.simonvt.cathode.entitymapper.ListItemMapper
import net.simonvt.cathode.entitymapper.UserListMapper
import net.simonvt.cathode.provider.ProviderSchematic.ListItems
import net.simonvt.cathode.provider.ProviderSchematic.Lists
import net.simonvt.cathode.provider.helper.ListDatabaseHelper
import net.simonvt.cathode.ui.RefreshableViewModel
import timber.log.Timber
import javax.inject.Inject

class ListViewModel @Inject constructor(
  private val context: Context,
  private val listHelper: ListDatabaseHelper,
  private val syncList: SyncList
) : RefreshableViewModel() {

  private var listId = -1L

  lateinit var list: LiveData<UserList>
    private set
  val listItems = MutableLiveData<List<ListItem>>()

  private lateinit var unsortedListItemsLiveData: LiveData<List<ListItem>>
  private var unsortedList: List<ListItem>? = null

  var sortBy: SortBy? = null
    set(value) {
      field = value
      unsortedList?.let { sortAndPostList(it) }
    }
  private var sortOrientation: SortOrientation? = null

  fun setListId(itemId: Long) {
    if (this.listId == -1L) {
      this.listId = itemId

      list = MappedCursorLiveData(
        context,
        Lists.withId(listId),
        UserListMapper.projection,
        null,
        null,
        null,
        UserListMapper
      )
      unsortedListItemsLiveData = MappedCursorLiveData(
        context,
        ListItems.inList(listId),
        ListItemMapper.projection,
        null,
        null,
        null,
        ListItemListMapper
      )

      list.observeForever(listObserver)
    }
  }

  override fun onCleared() {
    list.removeObserver(listObserver)
    unsortedListItemsLiveData.removeObserver(listItemsObserver)
    super.onCleared()
  }

  private val listObserver = object : Observer<UserList> {
    override fun onChanged(it: UserList) {
      if (sortBy == null) {
        list.removeObserver(this)
        sortBy = it.sortBy
        sortOrientation = it.sortOrientation
        unsortedListItemsLiveData.observeForever(listItemsObserver)
        Timber.d("Orientation: %s", sortOrientation.toString())
      }
    }
  }

  private var listItemsObserver = Observer<List<ListItem>> {
    unsortedList = it
    sortAndPostList(it)
  }

  private fun sortAndPostList(unsortedList: List<ListItem>) {
    val sortedList = if (sortBy == RANK) {
      unsortedList.sortedBy { it.rank }
    } else if (sortBy == ADDED) {
      unsortedList.sortedByDescending { it.listedAt }
    } else if (sortBy == TITLE) {
      unsortedList.sortedBy { getTitle(it) }
    } else if (sortBy == RELEASED) {
      unsortedList.sortedWith(Comparator { itemOne, itemTwo ->
        compareItems(
          itemOne,
          itemTwo,
          ::getReleased
        )
      })
    } else if (sortBy == RUNTIME) {
      unsortedList.sortedWith(Comparator { itemOne, itemTwo ->
        compareItems(
          itemOne,
          itemTwo,
          ::getRuntime
        )
      })
    } else if (sortBy == PERCENTAGE) {
      unsortedList.sortedWith(Comparator { itemOne, itemTwo ->
        compareItems(
          itemOne,
          itemTwo,
          ::getPercentage
        )
      })
    } else if (sortBy == VOTES || sortBy == POPULARITY) {
      unsortedList.sortedWith(Comparator { itemOne, itemTwo ->
        compareItems(
          itemOne,
          itemTwo,
          ::getVotes
        )
      })
    } else if (sortBy == MY_RATING) {
      unsortedList.sortedWith(Comparator { itemOne, itemTwo ->
        compareItems(
          itemOne,
          itemTwo,
          ::getUserRating
        )
      })
    } else if (sortBy == RANDOM) {
      unsortedList
    } else {
      unsortedList
    }

    // if (sortOrientation == SortOrientation.DESC) {
    //  sortedList = sortedList.asReversed()
    // }

    listItems.postValue(sortedList)
  }

  private fun <T : Comparable<T>> compareItems(
    itemOne: ListItem,
    itemTwo: ListItem,
    op: (ListItem) -> T
  ): Int {
    val itemOneValue = op(itemOne)
    val itemTwoValue = op(itemTwo)

    if (itemOneValue == itemTwoValue) {
      val itemOneTitle = getTitle(itemOne)
      val itemTwoTitle = getTitle(itemTwo)
      if (itemOneTitle == null && itemTwoTitle == null) {
        return itemOne.listItemId.compareTo(itemTwo.listItemId)
      } else if (itemOneTitle == null) {
        return 1
      } else if (itemTwoTitle == null) {
        return -1
      } else {
        return itemOneTitle.compareTo(itemTwoTitle)
      }
    } else {
      return itemTwoValue.compareTo(itemOneValue)
    }
  }

  private fun getTitle(listItem: ListItem): String? {
    return when (listItem.type) {
      SHOW -> listItem.show!!.titleNoArticle
      SEASON -> listItem.season!!.showTitleNoArticle
      EPISODE -> listItem.episode!!.showTitleNoArticle
      MOVIE -> listItem.movie!!.titleNoArticle
      PERSON -> listItem.person!!.name
      else -> throw IllegalArgumentException("Unsupported item type: " + listItem.type.value)
    }
  }

  private fun getReleased(listItem: ListItem): Long {
    return when (listItem.type) {
      SHOW -> listItem.show!!.firstAired
      SEASON -> listItem.season!!.firstAired
      EPISODE -> listItem.episode!!.firstAired
      MOVIE -> listItem.movie!!.releaseDate
      PERSON -> 0
      else -> throw IllegalArgumentException("Unsupported item type: " + listItem.type.value)
    }
  }

  private fun getRuntime(listItem: ListItem): Int {
    return when (listItem.type) {
      SHOW -> listItem.show!!.airedRuntime
      SEASON -> listItem.season!!.airedRuntime
      EPISODE -> listItem.episode!!.runtime
      MOVIE -> listItem.movie!!.runtime
      PERSON -> 0
      else -> throw IllegalArgumentException("Unsupported item type: " + listItem.type.value)
    }
  }

  private fun getPercentage(listItem: ListItem): Float {
    return when (listItem.type) {
      SHOW -> listItem.show!!.rating
      SEASON -> listItem.season!!.rating
      EPISODE -> listItem.episode!!.rating
      MOVIE -> listItem.movie!!.rating
      PERSON -> 0.0f
      else -> throw IllegalArgumentException("Unsupported item type: " + listItem.type.value)
    }
  }

  private fun getVotes(listItem: ListItem): Int {
    return when (listItem.type) {
      SHOW -> listItem.show!!.votes
      SEASON -> listItem.season!!.votes
      EPISODE -> listItem.episode!!.votes
      MOVIE -> listItem.movie!!.votes
      PERSON -> 0
      else -> throw IllegalArgumentException("Unsupported item type: " + listItem.type.value)
    }
  }

  private fun getUserRating(listItem: ListItem): Int {
    return when (listItem.type) {
      SHOW -> listItem.show!!.userRating
      SEASON -> listItem.season!!.userRating
      EPISODE -> listItem.episode!!.userRating
      MOVIE -> listItem.movie!!.userRating
      PERSON -> 0
      else -> throw IllegalArgumentException("Unsupported item type: " + listItem.type.value)
    }
  }

  override suspend fun onRefresh() {
    val traktId = listHelper.getTraktId(listId)
    syncList.invokeSync(SyncList.Params(traktId))
  }
}
