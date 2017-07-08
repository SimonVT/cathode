/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
import net.simonvt.cathode.api.body.ListInfoBody;
import net.simonvt.cathode.api.entity.CustomList;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class UpdateList extends CallJob<CustomList> {

  @Inject transient UsersService usersServie;

  private Long traktId;

  private String name;

  private String description;

  private Privacy privacy;

  private Boolean displayNumbers;

  private Boolean allowComments;

  public UpdateList(long traktId, String name, String description, Privacy privacy,
      Boolean displayNumbers, Boolean allowComments) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.name = name;
    this.description = description;
    this.privacy = privacy;
    this.displayNumbers = displayNumbers;
    this.allowComments = allowComments;
  }

  @Override public String key() {
    return "UpdateList&traktId=" + traktId;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return JobPriority.ACTIONS;
  }

  @Override public Call<CustomList> getCall() {
    return usersServie.updateList(traktId, ListInfoBody.name(name)
        .description(description)
        .privacy(privacy)
        .displayNumbers(displayNumbers)
        .allowComments(allowComments));
  }

  @Override public boolean handleResponse(CustomList list) {
    return true;
  }
}
