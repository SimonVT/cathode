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
import net.simonvt.cathode.common.entity.ListItem;
import net.simonvt.cathode.common.entity.UserList;
import net.simonvt.cathode.entitymapper.ListItemListMapper;
import net.simonvt.cathode.entitymapper.ListItemMapper;
import net.simonvt.cathode.entitymapper.UserListMapper;
import net.simonvt.cathode.provider.ProviderSchematic.ListItems;
import net.simonvt.cathode.provider.ProviderSchematic.Lists;

public class ListViewModel extends AndroidViewModel {

  private long listId = -1L;

  private LiveData<UserList> list;
  private LiveData<List<ListItem>> listItems;

  public ListViewModel(@NonNull Application application) {
    super(application);
  }

  public void setListId(long itemId) {
    if (this.listId == -1L) {
      this.listId = itemId;

      list = new MappedCursorLiveData<>(getApplication(), Lists.withId(listId),
          UserListMapper.PROJECTION, null, null, null, new UserListMapper());
      listItems = new MappedCursorLiveData<>(getApplication(), ListItems.inList(listId),
          ListItemMapper.PROJECTION, null, null, null, new ListItemListMapper());
    }
  }

  public LiveData<UserList> getList() {
    return list;
  }

  public LiveData<List<ListItem>> getListItems() {
    return listItems;
  }
}
