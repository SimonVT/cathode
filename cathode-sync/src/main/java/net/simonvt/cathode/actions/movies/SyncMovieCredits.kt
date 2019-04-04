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

package net.simonvt.cathode.actions.movies

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.movies.SyncMovieCredits.Params
import net.simonvt.cathode.api.entity.CrewMember
import net.simonvt.cathode.api.entity.People
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.MoviesService
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast
import net.simonvt.cathode.provider.ProviderSchematic.MovieCrew
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import retrofit2.Call
import java.util.ArrayList
import javax.inject.Inject

class SyncMovieCredits @Inject constructor(
  private val context: Context,
  private val movieHelper: MovieDatabaseHelper,
  private val personHelper: PersonDatabaseHelper,
  private val moviesService: MoviesService
) : CallAction<Params, People>() {

  override fun key(params: Params): String = "SyncMovieCredits&traktId=${params.traktId}"

  override fun getCall(params: Params): Call<People> =
    moviesService.getPeople(params.traktId, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: People) {
    val movieId = movieHelper.getId(params.traktId)
    val ops = arrayListOf<ContentProviderOperation>()

    var op = ContentProviderOperation.newDelete(MovieCast.fromMovie(movieId)).build()
    ops.add(op)
    op = ContentProviderOperation.newDelete(MovieCrew.fromMovie(movieId)).build()
    ops.add(op)

    val cast = response.cast
    if (cast != null) {
      for ((character, person) in cast) {
        val personId = personHelper.partialUpdate(person)

        op = ContentProviderOperation.newInsert(MovieCast.MOVIE_CAST)
          .withValue(MovieCastColumns.MOVIE_ID, movieId)
          .withValue(MovieCastColumns.PERSON_ID, personId)
          .withValue(MovieCastColumns.CHARACTER, character)
          .build()
        ops.add(op)
      }
    }

    val crew = response.crew
    if (crew != null) {
      insertCrew(ops, movieId, Department.PRODUCTION, crew.production)
      insertCrew(ops, movieId, Department.ART, crew.art)
      insertCrew(ops, movieId, Department.CREW, crew.crew)
      insertCrew(ops, movieId, Department.COSTUME_AND_MAKEUP, crew.costume_and_make_up)
      insertCrew(ops, movieId, Department.DIRECTING, crew.directing)
      insertCrew(ops, movieId, Department.WRITING, crew.writing)
      insertCrew(ops, movieId, Department.SOUND, crew.sound)
      insertCrew(ops, movieId, Department.CAMERA, crew.camera)
    }

    ops.add(
      ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValue(
        MovieColumns.LAST_CREDITS_SYNC,
        System.currentTimeMillis()
      ).build()
    )

    context.contentResolver.batch(ops)
  }

  private fun insertCrew(
    ops: ArrayList<ContentProviderOperation>,
    movieId: Long,
    department: Department,
    crew: List<CrewMember>?
  ) {
    if (crew == null) {
      return
    }
    for ((job, person) in crew) {
      val personId = personHelper.partialUpdate(person)

      val op = ContentProviderOperation.newInsert(MovieCrew.MOVIE_CREW)
        .withValue(MovieCrewColumns.MOVIE_ID, movieId)
        .withValue(MovieCrewColumns.PERSON_ID, personId)
        .withValue(MovieCrewColumns.CATEGORY, department.toString())
        .withValue(MovieCrewColumns.JOB, job)
        .build()
      ops.add(op)
    }
  }

  data class Params(val traktId: Long)
}
