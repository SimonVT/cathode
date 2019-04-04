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

package net.simonvt.cathode.actions.people

import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.people.SyncPerson.Params
import net.simonvt.cathode.api.entity.Person
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.PeopleService
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import retrofit2.Call
import javax.inject.Inject

class SyncPerson @Inject constructor(
  private val personHelper: PersonDatabaseHelper,
  private val peopleService: PeopleService
) : CallAction<Params, Person>() {

  override fun key(params: Params): String = "SyncPerson&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<Person> =
    peopleService.summary(params.traktId, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: Person) {
    personHelper.fullUpdate(response)
  }

  data class Params(val traktId: Long)
}
