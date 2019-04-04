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

package net.simonvt.cathode.actions.people

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.people.SyncPersonShowCredits.Params
import net.simonvt.cathode.api.entity.Credit
import net.simonvt.cathode.api.entity.Credits
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.PeopleService
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowCrewColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.ShowCast
import net.simonvt.cathode.provider.ProviderSchematic.ShowCrew
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import java.util.ArrayList
import javax.inject.Inject

class SyncPersonShowCredits @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val personHelper: PersonDatabaseHelper,
  private val peopleService: PeopleService
) : CallAction<Params, Credits>() {

  override fun key(params: Params): String = "SyncPersonShowCredits&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<Credits> =
    peopleService.shows(params.traktId, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: Credits) {
    val ops = arrayListOf<ContentProviderOperation>()
    val personId = personHelper.getId(params.traktId)

    val oldCastCursor = context.contentResolver.query(
      ShowCast.withPerson(personId),
      arrayOf(Tables.SHOW_CAST + "." + ShowCastColumns.ID, ShowCastColumns.SHOW_ID)
    )
    val oldCast = mutableListOf<Long>()
    val showToCastIdMap = mutableMapOf<Long, Long>()
    while (oldCastCursor.moveToNext()) {
      val id = Cursors.getLong(oldCastCursor, ShowCastColumns.ID)
      val showId = Cursors.getLong(oldCastCursor, ShowCastColumns.SHOW_ID)
      oldCast.add(showId)
      showToCastIdMap[showId] = id
    }
    oldCastCursor.close()

    val cast = response.cast
    if (cast != null) {
      for (credit in cast) {
        val showId = showHelper.partialUpdate(credit.show!!)

        if (oldCast.remove(showId)) {
          val castId = showToCastIdMap[showId]!!
          val op = ContentProviderOperation.newUpdate(ShowCast.withId(castId))
            .withValue(ShowCastColumns.SHOW_ID, showId)
            .withValue(ShowCastColumns.PERSON_ID, personId)
            .withValue(ShowCastColumns.CHARACTER, credit.character)
            .build()
          ops.add(op)
        } else {
          val op = ContentProviderOperation.newInsert(ShowCast.SHOW_CAST)
            .withValue(ShowCastColumns.SHOW_ID, showId)
            .withValue(ShowCastColumns.PERSON_ID, personId)
            .withValue(ShowCastColumns.CHARACTER, credit.character)
            .build()
          ops.add(op)
        }
      }
    }

    for (showId in oldCast) {
      val castId = showToCastIdMap[showId]!!
      val op = ContentProviderOperation.newDelete(ShowCast.withId(castId)).build()
      ops.add(op)
    }

    val crew = response.crew
    if (crew != null) {
      insertCrew(ops, personId, Department.PRODUCTION, crew.production)
      insertCrew(ops, personId, Department.ART, crew.art)
      insertCrew(ops, personId, Department.CREW, crew.crew)
      insertCrew(ops, personId, Department.COSTUME_AND_MAKEUP, crew.costume_and_make_up)
      insertCrew(ops, personId, Department.DIRECTING, crew.directing)
      insertCrew(ops, personId, Department.WRITING, crew.writing)
      insertCrew(ops, personId, Department.SOUND, crew.sound)
      insertCrew(ops, personId, Department.CAMERA, crew.camera)
    }

    context.contentResolver.batch(ops)
  }

  private fun insertCrew(
    ops: ArrayList<ContentProviderOperation>,
    personId: Long,
    department: Department,
    crew: List<Credit>?
  ) {
    val oldCrewCursor = context.contentResolver.query(
      ShowCrew.withPerson(personId),
      arrayOf(Tables.SHOW_CREW + "." + ShowCrewColumns.ID, ShowCrewColumns.SHOW_ID),
      ShowCrewColumns.CATEGORY + "=?",
      arrayOf(department.toString())
    )
    val oldCrew = mutableListOf<Long>()
    val showToCrewIdMap = mutableMapOf<Long, Long>()
    while (oldCrewCursor.moveToNext()) {
      val id = Cursors.getLong(oldCrewCursor, ShowCrewColumns.ID)
      val showId = Cursors.getLong(oldCrewCursor, ShowCrewColumns.SHOW_ID)
      oldCrew.add(showId)
      showToCrewIdMap[showId] = id
    }
    oldCrewCursor.close()

    if (crew != null) {
      for (credit in crew) {
        val showId = showHelper.partialUpdate(credit.show!!)

        if (oldCrew.remove(showId)) {
          val crewId = showToCrewIdMap[showId]!!
          val op = ContentProviderOperation.newUpdate(ShowCrew.withId(crewId))
            .withValue(ShowCrewColumns.SHOW_ID, showId)
            .withValue(ShowCrewColumns.PERSON_ID, personId)
            .withValue(ShowCrewColumns.CATEGORY, department.toString())
            .withValue(ShowCrewColumns.JOB, credit.job)
            .build()
          ops.add(op)
        } else {
          val op = ContentProviderOperation.newInsert(ShowCrew.SHOW_CREW)
            .withValue(ShowCrewColumns.SHOW_ID, showId)
            .withValue(ShowCrewColumns.PERSON_ID, personId)
            .withValue(ShowCrewColumns.CATEGORY, department.toString())
            .withValue(ShowCrewColumns.JOB, credit.job)
            .build()
          ops.add(op)
        }
      }
    }

    for (showId in oldCrew) {
      val crewId = showToCrewIdMap[showId]!!
      val op = ContentProviderOperation.newDelete(ShowCrew.withId(crewId)).build()
      ops.add(op)
    }
  }

  data class Params(val traktId: Long)
}
