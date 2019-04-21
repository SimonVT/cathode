/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.entity.CastMember
import net.simonvt.cathode.entity.Person
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables

object MovieCastMapper : MappedCursorLiveData.CursorMapper<List<CastMember>> {

  override fun map(cursor: Cursor): List<CastMember> {
    val castMembers = mutableListOf<CastMember>()

    cursor.moveToPosition(-1)
    while (cursor.moveToNext()) {
      val personId = cursor.getLong(MovieCastColumns.PERSON_ID)
      val personName = cursor.getString(PersonColumns.NAME)
      val person =
        Person(personId, personName, null, null, null, null, null)

      val character = cursor.getString(MovieCastColumns.CHARACTER)
      castMembers.add(CastMember(character, person))
    }

    return castMembers
  }

  val projection = arrayOf(
    Tables.MOVIE_CAST + "." + MovieCastColumns.ID,
    Tables.MOVIE_CAST + "." + MovieCastColumns.PERSON_ID,
    Tables.MOVIE_CAST + "." + MovieCastColumns.CHARACTER,
    Tables.PEOPLE + "." + PersonColumns.NAME
  )
}
