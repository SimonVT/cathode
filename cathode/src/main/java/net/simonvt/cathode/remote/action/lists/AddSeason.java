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

public class AddSeason extends Job {

  @Inject transient UsersService usersService;

  private long listId;

  private long showTraktId;

  private int seasonNumber;

  public AddSeason(long listId, long showTraktId, int seasonNumber) {
    super(Flags.REQUIRES_AUTH);
    this.listId = listId;
    this.showTraktId = showTraktId;
    this.seasonNumber = seasonNumber;
  }

  @Override public String key() {
    return "AddSeason&listId=" + listId + "?traktId=" + showTraktId + "?seasonNumber="
        + seasonNumber;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public void perform() {
    ListItemActionBody body = new ListItemActionBody();
    body.show(showTraktId).season(seasonNumber);
    usersService.addItems(listId, body);
  }
}
