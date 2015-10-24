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
import net.simonvt.cathode.provider.PersonWrapper;
import net.simonvt.cathode.remote.CallJob;
import retrofit.Call;

public class SyncPerson extends CallJob<Person> {

  @Inject transient PeopleService peopleService;

  private long traktId;

  public SyncPerson(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncPerson" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_EXTRAS;
  }

  @Override public Call<Person> getCall() {
    return peopleService.summary(traktId, Extended.FULL_IMAGES);
  }

  @Override public void handleResponse(Person person) {
    PersonWrapper.updateOrInsert(getContentResolver(), person);
  }
}
