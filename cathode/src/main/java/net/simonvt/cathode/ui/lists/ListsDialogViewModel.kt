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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.UserList
import net.simonvt.cathode.entitymapper.UserListListMapper
import net.simonvt.cathode.entitymapper.UserListMapper
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns
import net.simonvt.cathode.provider.ProviderSchematic.ListItems
import net.simonvt.cathode.provider.ProviderSchematic.Lists
import net.simonvt.cathode.ui.lists.DialogListItemListMapper.DialogListItem

class ListsDialogViewModel(application: Application) : AndroidViewModel(application) {

  private lateinit var itemType: ItemType
  private var itemId = -1L

  lateinit var lists: LiveData<List<UserList>>
  lateinit var listItems: LiveData<List<DialogListItem>>

  fun setItemTypeAndId(itemType: ItemType, itemId: Long) {
    if (this.itemId == -1L) {
      this.itemType = itemType
      this.itemId = itemId

      lists = MappedCursorLiveData(
        getApplication(),
        Lists.LISTS,
        UserListMapper.projection,
        null,
        null,
        null,
        UserListListMapper
      )
      listItems = MappedCursorLiveData(
        getApplication(),
        ListItems.LIST_ITEMS,
        DialogListItemListMapper.PROJECTION,
        ListItemColumns.ITEM_TYPE + "=? AND " + ListItemColumns.ITEM_ID + "=?",
        arrayOf(itemType.toString(), itemId.toString()),
        null,
        DialogListItemListMapper()
      )
    }
  }
}
