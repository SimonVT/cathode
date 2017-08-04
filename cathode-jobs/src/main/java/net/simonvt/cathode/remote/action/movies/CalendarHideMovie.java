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

package net.simonvt.cathode.remote.action.movies;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.HiddenItems;
import net.simonvt.cathode.api.entity.HideResponse;
import net.simonvt.cathode.api.enumeration.HiddenSection;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import retrofit2.Call;

public class CalendarHideMovie extends CallJob<HideResponse> {

  @Inject transient UsersService usersService;

  private long traktId;
  private boolean hidden;

  public CalendarHideMovie(long traktId, boolean hidden) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.hidden = hidden;
  }

  @Override public String key() {
    return "CalendarHideMovie?traktId=" + traktId + "&hidden=" + hidden;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return JobPriority.ACTIONS;
  }

  @Override public Call<HideResponse> getCall() {
    HiddenItems items = new HiddenItems();
    items.movie(traktId);

    if (hidden) {
      return usersService.addHiddenItems(HiddenSection.CALENDAR, items);
    } else {
      return usersService.removeHiddenItems(HiddenSection.CALENDAR, items);
    }
  }

  @Override public boolean handleResponse(HideResponse response) {
    queue(new SyncUserActivity());
    return true;
  }
}
