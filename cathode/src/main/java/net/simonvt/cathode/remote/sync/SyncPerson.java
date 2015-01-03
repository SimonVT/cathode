/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.remote.sync;

import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.PeopleService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.PersonWrapper;

public class SyncPerson extends Job {

  @Inject transient PeopleService peopleService;

  private long traktId;

  public SyncPerson(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncPerson" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_1;
  }

  @Override public void perform() {
    Person person = peopleService.summary(traktId, Extended.FULL_IMAGES);
    PersonWrapper.updateOrInsert(getContentResolver(), person);
  }
}
