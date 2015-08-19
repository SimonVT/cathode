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
import net.simonvt.cathode.api.body.ListItemActionBody;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.Flags;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class RemovePerson extends Job {

  @Inject transient UsersService usersService;

  private long listId;

  private long traktId;

  public RemovePerson(long listId, long traktId) {
    super(Flags.REQUIRES_AUTH);
    this.listId = listId;
    this.traktId = traktId;
  }

  @Override public String key() {
    return "RemovePerson&listId=" + listId + "?traktId=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public void perform() {
    try {
      ListItemActionBody body = new ListItemActionBody();
      body.person(traktId);
      usersService.removeItem(listId, body);
    } catch (RetrofitError error) {
      Response response = error.getResponse();
      // The trakt API has a bug where removing a person returns a 500 status code.
      if (response == null || response.getStatus() != 500) {
        throw error;
      }
    }
  }
}
