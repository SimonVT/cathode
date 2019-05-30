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
import net.simonvt.cathode.actions.people.SyncPersonMovieCredits.Params
import net.simonvt.cathode.api.entity.Credit
import net.simonvt.cathode.api.entity.Credits
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.PeopleService
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast
import net.simonvt.cathode.provider.ProviderSchematic.MovieCrew
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import java.util.ArrayList
import javax.inject.Inject

class SyncPersonMovieCredits @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val personHelper: PersonDatabaseHelper,
  private val peopleService: PeopleService
) : CallAction<Params, Credits>() {

  override fun key(params: Params): String = "SyncPersonMovieCredits&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<Credits> =
    peopleService.movies(params.traktId, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: Credits) {
    val ops = arrayListOf<ContentProviderOperation>()
    val personId = personHelper.getId(params.traktId)

    val oldCastCursor = context.contentResolver.query(
      MovieCast.withPerson(personId),
      arrayOf(Tables.MOVIE_CAST + "." + MovieCastColumns.ID, MovieCastColumns.MOVIE_ID)
    )
    val oldCast = mutableListOf<Long>()
    val movieToCastIdMap = mutableMapOf<Long, Long>()
    while (oldCastCursor.moveToNext()) {
      val id = oldCastCursor.getLong(MovieCastColumns.ID)
      val movieId = oldCastCursor.getLong(MovieCastColumns.MOVIE_ID)
      oldCast.add(movieId)
      movieToCastIdMap[movieId] = id
    }
    oldCastCursor.close()

    val cast = response.cast
    if (cast != null) {
      for (credit in cast) {
        val movieId = movieHelper.partialUpdate(credit.movie!!)

        if (oldCast.remove(movieId)) {
          val castId = movieToCastIdMap[movieId]!!
          val op = ContentProviderOperation.newUpdate(MovieCast.withId(castId))
            .withValue(MovieCastColumns.MOVIE_ID, movieId)
            .withValue(MovieCastColumns.PERSON_ID, personId)
            .withValue(MovieCastColumns.CHARACTER, credit.character)
            .build()
          ops.add(op)
        } else {
          val op = ContentProviderOperation.newInsert(MovieCast.MOVIE_CAST)
            .withValue(MovieCastColumns.MOVIE_ID, movieId)
            .withValue(MovieCastColumns.PERSON_ID, personId)
            .withValue(MovieCastColumns.CHARACTER, credit.character)
            .build()
          ops.add(op)
        }
      }
    }

    for (movieId in oldCast) {
      val castId = movieToCastIdMap[movieId]!!
      val op = ContentProviderOperation.newDelete(MovieCast.withId(castId)).build()
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
      MovieCrew.withPerson(personId),
      arrayOf(Tables.MOVIE_CREW + "." + MovieCrewColumns.ID, MovieCrewColumns.MOVIE_ID),
      MovieCrewColumns.CATEGORY + "=?",
      arrayOf(department.toString())
    )
    val oldCrew = mutableListOf<Long>()
    val movieToCrewIdMap = mutableMapOf<Long, Long>()
    while (oldCrewCursor.moveToNext()) {
      val id = oldCrewCursor.getLong(MovieCrewColumns.ID)
      val movieId = oldCrewCursor.getLong(MovieCrewColumns.MOVIE_ID)
      oldCrew.add(movieId)
      movieToCrewIdMap[movieId] = id
    }
    oldCrewCursor.close()

    if (crew != null) {
      for (credit in crew) {
        val movieId = movieHelper.partialUpdate(credit.movie!!)

        if (oldCrew.remove(movieId)) {
          val crewId = movieToCrewIdMap.get(movieId)!!
          val op = ContentProviderOperation.newUpdate(MovieCrew.withId(crewId))
            .withValue(MovieCrewColumns.MOVIE_ID, movieId)
            .withValue(MovieCrewColumns.PERSON_ID, personId)
            .withValue(MovieCrewColumns.CATEGORY, department.toString())
            .withValue(MovieCrewColumns.JOB, credit.job)
            .build()
          ops.add(op)
        } else {
          val op = ContentProviderOperation.newInsert(MovieCrew.MOVIE_CREW)
            .withValue(MovieCrewColumns.MOVIE_ID, movieId)
            .withValue(MovieCrewColumns.PERSON_ID, personId)
            .withValue(MovieCrewColumns.CATEGORY, department.toString())
            .withValue(MovieCrewColumns.JOB, credit.job)
            .build()
          ops.add(op)
        }
      }
    }

    for (movieId in oldCrew) {
      val crewId = movieToCrewIdMap[movieId]!!

      val op = ContentProviderOperation.newDelete(MovieCrew.withId(crewId)).build()
      ops.add(op)
    }
  }

  data class Params(val traktId: Long)
}
