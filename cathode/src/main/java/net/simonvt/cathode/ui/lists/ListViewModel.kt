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
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.lists.SyncList
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.entity.ListItem
import net.simonvt.cathode.common.entity.UserList
import net.simonvt.cathode.entitymapper.ListItemListMapper
import net.simonvt.cathode.entitymapper.ListItemMapper
import net.simonvt.cathode.entitymapper.UserListMapper
import net.simonvt.cathode.provider.ProviderSchematic.ListItems
import net.simonvt.cathode.provider.ProviderSchematic.Lists
import net.simonvt.cathode.provider.helper.ListWrapper
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class ListViewModel @Inject constructor(
  private val context: Context,
  private val syncList: SyncList
) : RefreshableViewModel() {

  private var listId = -1L

  lateinit var list: LiveData<UserList>
    private set
  lateinit var listItems: LiveData<List<ListItem>>
    private set

  fun setListId(itemId: Long) {
    if (this.listId == -1L) {
      this.listId = itemId

      list = MappedCursorLiveData(
        context,
        Lists.withId(listId),
        UserListMapper.PROJECTION,
        null,
        null,
        null,
        UserListMapper()
      )
      listItems = MappedCursorLiveData(
        context,
        ListItems.inList(listId),
        ListItemMapper.PROJECTION,
        null,
        null,
        null,
        ListItemListMapper()
      )
    }
  }

  override suspend fun onRefresh() {
    val traktId = ListWrapper.getTraktId(context.contentResolver, listId)
    ActionManager.invokeSync(SyncList.key(traktId), syncList, SyncList.Params(traktId))
  }
}
