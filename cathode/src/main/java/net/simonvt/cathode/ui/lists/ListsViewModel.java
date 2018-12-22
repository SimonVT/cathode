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
import net.simonvt.cathode.provider.ProviderSchematic.Lists;

public class ListsViewModel extends AndroidViewModel {

  private LiveData<List<UserList>> lists;

  public ListsViewModel(@NonNull Application application) {
    super(application);
    lists =
        new MappedCursorLiveData<>(application, Lists.LISTS, UserListMapper.PROJECTION, null, null,
            null, new UserListListMapper());
  }

  public LiveData<List<UserList>> getLists() {
    return lists;
  }
}
