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

package net.simonvt.cathode.ui.lists;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.UserList;
import net.simonvt.cathode.entitymapper.UserListListMapper;
import net.simonvt.cathode.entitymapper.UserListMapper;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.ProviderSchematic.ListItems;
import net.simonvt.cathode.provider.ProviderSchematic.Lists;
import net.simonvt.cathode.ui.lists.DialogListItemListMapper.DialogListItem;

public class ListsDialogViewModel extends AndroidViewModel {

  private int itemType;
  private long itemId = -1L;

  private LiveData<List<UserList>> lists;
  private LiveData<List<DialogListItem>> listItems;

  public ListsDialogViewModel(@NonNull Application application) {
    super(application);
  }

  public void setItemTypeAndId(int itemType, long itemId) {
    if (this.itemId == -1L) {
      this.itemType = itemType;
      this.itemId = itemId;

      lists =
          new MappedCursorLiveData<>(getApplication(), Lists.LISTS, UserListMapper.PROJECTION, null,
              null, null, new UserListListMapper());
      listItems = new MappedCursorLiveData<>(getApplication(), ListItems.LIST_ITEMS,
          DialogListItemListMapper.PROJECTION,
          ListItemColumns.ITEM_TYPE + "=? AND " + ListItemColumns.ITEM_ID + "=?",
          new String[] { String.valueOf(itemType), String.valueOf(itemId), }, null,
          new DialogListItemListMapper());
    }
  }

  public LiveData<List<UserList>> getLists() {
    return lists;
  }

  public LiveData<List<DialogListItem>> getListItems() {
    return listItems;
  }
}
