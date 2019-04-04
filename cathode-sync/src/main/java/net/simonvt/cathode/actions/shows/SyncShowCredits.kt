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

package net.simonvt.cathode.actions.shows

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.shows.SyncShowCredits.Params
import net.simonvt.cathode.api.entity.CrewMember
import net.simonvt.cathode.api.entity.People
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.ShowsService
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowCrewColumns
import net.simonvt.cathode.provider.ProviderSchematic.ShowCast
import net.simonvt.cathode.provider.ProviderSchematic.ShowCrew
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import retrofit2.Call
import javax.inject.Inject

class SyncShowCredits @Inject constructor(
  private val context: Context,
  private val showsService: ShowsService,
  private val showHelper: ShowDatabaseHelper,
  private val personHelper: PersonDatabaseHelper
) : CallAction<Params, People>() {

  override fun key(params: Params): String = "SyncShowCredits&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<People> =
    showsService.getPeople(params.traktId, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: People) {
    val showId = showHelper.getId(params.traktId)

    val ops = arrayListOf<ContentProviderOperation>()
    ops.add(ContentProviderOperation.newDelete(ShowCast.fromShow(showId)).build())
    ops.add(ContentProviderOperation.newDelete(ShowCrew.fromShow(showId)).build())

    response.cast?.forEach { character ->
      val person = character.person
      val personId = personHelper.partialUpdate(person)

      val op = ContentProviderOperation.newInsert(ShowCast.SHOW_CAST)
        .withValue(ShowCastColumns.SHOW_ID, showId)
        .withValue(ShowCastColumns.PERSON_ID, personId)
        .withValue(ShowCastColumns.CHARACTER, character.character)
        .build()
      ops.add(op)
    }

    response.crew?.apply {
      insertCrew(ops, showId, Department.PRODUCTION, production)
      insertCrew(ops, showId, Department.ART, art)
      insertCrew(ops, showId, Department.CREW, crew)
      insertCrew(ops, showId, Department.COSTUME_AND_MAKEUP, costume_and_make_up)
      insertCrew(ops, showId, Department.DIRECTING, directing)
      insertCrew(ops, showId, Department.WRITING, writing)
      insertCrew(ops, showId, Department.SOUND, sound)
      insertCrew(ops, showId, Department.CAMERA, camera)
    }

    ops.add(
      ContentProviderOperation.newUpdate(Shows.withId(showId))
        .withValue(ShowColumns.LAST_CREDITS_SYNC, System.currentTimeMillis())
        .build()
    )

    context.contentResolver.batch(ops)
  }

  private fun insertCrew(
    ops: MutableList<ContentProviderOperation>,
    showId: Long,
    department: Department,
    crew: List<CrewMember>?
  ) {
    if (crew != null) {
      for (crewMember in crew) {
        val personId = personHelper.partialUpdate(crewMember.person)

        val op = ContentProviderOperation.newInsert(ShowCrew.SHOW_CREW)
          .withValue(ShowCrewColumns.SHOW_ID, showId)
          .withValue(ShowCrewColumns.CATEGORY, department.toString())
          .withValue(ShowCrewColumns.PERSON_ID, personId)
          .withValue(ShowCrewColumns.JOB, crewMember.job)
          .build()
        ops.add(op)
      }
    }
  }

  data class Params(val traktId: Long)
}
