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
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.ProviderSchematic.ListItems;
import net.simonvt.cathode.provider.ProviderSchematic.Lists;

public class ListsDialogViewModel extends AndroidViewModel {

  private int itemType;
  private long itemId = -1L;

  private LiveData<Cursor> list;
  private LiveData<Cursor> listItems;

  public ListsDialogViewModel(@NonNull Application application) {
    super(application);
  }

  public void setItemTypeAndId(int itemType, long itemId) {
    if (this.itemId == -1L) {
      this.itemType = itemType;
      this.itemId = itemId;

      list = new CursorLiveData(getApplication(), Lists.LISTS, ListsDialog.LISTS_PROJECTION, null,
          null, null);
      listItems = new CursorLiveData(getApplication(), ListItems.LIST_ITEMS,
          ListsDialog.LIST_ITEM_PROJECTION,
          ListItemColumns.ITEM_TYPE + "=? AND " + ListItemColumns.ITEM_ID + "=?", new String[] {
          String.valueOf(itemType), String.valueOf(itemId),
      }, null);
    }
  }

  public LiveData<Cursor> getList() {
    return list;
  }

  public LiveData<Cursor> getListItems() {
    return listItems;
  }
}
