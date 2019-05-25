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

package net.simonvt.cathode.remote.action.lists;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.IdsBody;
import net.simonvt.cathode.api.entity.ListItemActionResponse;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import retrofit2.Call;

public class AddMovie extends CallJob<ListItemActionResponse> {

  @Inject transient UsersService usersService;

  private long listId;
  private long traktId;

  public AddMovie(long listId, long traktId) {

    this.listId = listId;
    this.traktId = traktId;
  }

  @Override public String key() {
    return "AddMovie&listId=" + listId + "?traktId=" + traktId;
  }

  @Override public Call<ListItemActionResponse> getCall() {
    return usersService.addItems(listId, new IdsBody.Builder().movie(traktId).build());
  }

  @Override public boolean handleResponse(ListItemActionResponse response) {
    queue(new SyncUserActivity());
    return true;
  }
}
