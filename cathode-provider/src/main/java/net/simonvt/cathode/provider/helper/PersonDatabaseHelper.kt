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

package net.simonvt.cathode.provider.helper

import android.content.ContentValues
import android.content.Context
import net.simonvt.cathode.api.entity.Person
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns
import net.simonvt.cathode.provider.ProviderSchematic.People
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonDatabaseHelper @Inject constructor(private val context: Context) {

  fun getId(traktId: Long): Long {
    val c = context.contentResolver.query(
      People.PEOPLE,
      arrayOf(PersonColumns.ID),
      PersonColumns.TRAKT_ID + "=?",
      arrayOf(traktId.toString())
    )
    val id = if (!c.moveToFirst()) -1L else c.getLong(PersonColumns.ID)
    c.close()
    return id
  }

  fun getIdFromTmdb(tmdbId: Int): Long {
    val c = context.contentResolver.query(
      People.PEOPLE,
      arrayOf(PersonColumns.ID),
      PersonColumns.TMDB_ID + "=?",
      arrayOf(tmdbId.toString())
    )
    val id = if (!c.moveToFirst()) -1L else c.getLong(PersonColumns.ID)
    c.close()
    return id
  }

  fun getTraktId(personId: Long): Long {
    val c = context.contentResolver.query(People.withId(personId), arrayOf(PersonColumns.TRAKT_ID))
    val id = if (!c.moveToFirst()) -1L else c.getLong(PersonColumns.TRAKT_ID)
    c.close()
    return id
  }

  fun getTmdbId(personId: Long): Int {
    val c = context.contentResolver.query(People.withId(personId), arrayOf(PersonColumns.TMDB_ID))
    val id = if (!c.moveToFirst()) -1 else c.getInt(PersonColumns.TMDB_ID)
    c.close()
    return id
  }

  private fun createPerson(traktId: Long): Long {
    val values = ContentValues()
    values.put(PersonColumns.TRAKT_ID, traktId)
    values.put(PersonColumns.NEEDS_SYNC, true)

    return People.getId(context.contentResolver.insert(People.PEOPLE, values)!!)
  }

  fun getIdOrCreate(person: Person): Long {
    synchronized(LOCK_ID) {
      val traktId = person.ids.trakt!!
      val personId = getId(traktId)
      return if (personId == -1L) {
        createPerson(traktId)
      } else {
        personId
      }
    }
  }

  fun partialUpdate(person: Person): Long {
    val id = getIdOrCreate(person)
    val values = getPartialValues(person)
    context.contentResolver.update(People.withId(id), values)
    return id
  }

  fun fullUpdate(person: Person): Long {
    val id = getIdOrCreate(person)

    val values = getValues(person)
    values.put(PersonColumns.NEEDS_SYNC, false)
    context.contentResolver.update(People.withId(id), values)

    return id
  }

  private fun getPartialValues(person: Person): ContentValues {
    val values = ContentValues()
    values.put(PersonColumns.NAME, person.name)
    values.put(PersonColumns.TRAKT_ID, person.ids.trakt)
    values.put(PersonColumns.SLUG, person.ids.slug)
    values.put(PersonColumns.IMDB_ID, person.ids.imdb)
    values.put(PersonColumns.TMDB_ID, person.ids.tmdb)
    values.put(PersonColumns.TVRAGE_ID, person.ids.tvrage)
    return values
  }

  private fun getValues(person: Person): ContentValues {
    val values = ContentValues()
    values.put(PersonColumns.NAME, person.name)
    values.put(PersonColumns.TRAKT_ID, person.ids.trakt)
    values.put(PersonColumns.SLUG, person.ids.slug)
    values.put(PersonColumns.IMDB_ID, person.ids.imdb)
    values.put(PersonColumns.TMDB_ID, person.ids.tmdb)
    values.put(PersonColumns.TVRAGE_ID, person.ids.tvrage)
    values.put(PersonColumns.BIOGRAPHY, person.biography)
    values.put(PersonColumns.BIRTHDAY, person.birthday)
    values.put(PersonColumns.DEATH, person.death)
    values.put(PersonColumns.BIRTHPLACE, person.birthplace)
    values.put(PersonColumns.HOMEPAGE, person.homepage)
    return values
  }

  companion object {

    private val LOCK_ID = Any()
  }
}
