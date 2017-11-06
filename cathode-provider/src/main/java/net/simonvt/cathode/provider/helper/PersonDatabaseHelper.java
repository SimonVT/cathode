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

package net.simonvt.cathode.provider.helper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.People;
import net.simonvt.schematic.Cursors;

public final class PersonDatabaseHelper {

  private Context context;

  private ContentResolver resolver;

  public PersonDatabaseHelper(Context context) {
    this.context = context;
    resolver = context.getContentResolver();
  }

  public long getId(long traktId) {
    Cursor c = resolver.query(People.PEOPLE, new String[] {
        PersonColumns.ID,
    }, PersonColumns.TRAKT_ID + "=?", new String[] {
        String.valueOf(traktId),
    }, null);

    long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, PersonColumns.ID);

    c.close();

    return id;
  }

  public long getIdFromTmdb(int tmdbId) {
    Cursor c = resolver.query(People.PEOPLE, new String[] {
        PersonColumns.ID,
    }, PersonColumns.TMDB_ID + "=?", new String[] {
        String.valueOf(tmdbId),
    }, null);

    long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, PersonColumns.ID);

    c.close();

    return id;
  }

  public long getTraktId(long personId) {
    Cursor c = resolver.query(People.withId(personId), new String[] {
        PersonColumns.TRAKT_ID,
    }, null, null, null);

    long id = !c.moveToFirst() ? -1L : Cursors.getLong(c, PersonColumns.TRAKT_ID);

    c.close();

    return id;
  }

  public int getTmdbId(long personId) {
    Cursor c = resolver.query(People.withId(personId), new String[] {
        PersonColumns.TMDB_ID,
    }, null, null, null);

    int id = !c.moveToFirst() ? -1 : Cursors.getInt(c, PersonColumns.TMDB_ID);

    c.close();

    return id;
  }

  public long createPerson(long traktId) {
    ContentValues values = new ContentValues();
    values.put(PersonColumns.TRAKT_ID, traktId);
    values.put(PersonColumns.NEEDS_SYNC, true);

    return People.getId(resolver.insert(People.PEOPLE, values));
  }

  public long updateOrInsert(Person person) {
    final long traktId = person.getIds().getTrakt();
    long personId = getId(traktId);

    if (personId == -1L) {
      personId = People.getId(resolver.insert(People.PEOPLE, getValues(person)));
    } else {
      resolver.update(People.withId(personId), getValues(person), null, null);
    }

    return personId;
  }

  private static ContentValues getValues(Person person) {
    ContentValues values = new ContentValues();

    values.put(PersonColumns.NAME, person.getName());
    values.put(PersonColumns.TRAKT_ID, person.getIds().getTrakt());
    values.put(PersonColumns.SLUG, person.getIds().getSlug());
    values.put(PersonColumns.IMDB_ID, person.getIds().getImdb());
    values.put(PersonColumns.TMDB_ID, person.getIds().getTmdb());
    values.put(PersonColumns.TVRAGE_ID, person.getIds().getTvrage());

    values.put(PersonColumns.BIOGRAPHY, person.getBiography());
    if (person.getBirthday() != null) {
      values.put(PersonColumns.BIRTHDAY, person.getBirthday());
    } else {
      values.putNull(PersonColumns.BIRTHDAY);
    }
    if (person.getDeath() != null) {
      values.put(PersonColumns.DEATH, person.getDeath());
    } else {
      values.putNull(PersonColumns.DEATH);
    }
    values.put(PersonColumns.BIRTHPLACE, person.getBirthplace());
    values.put(PersonColumns.HOMEPAGE, person.getHomepage());

    values.put(PersonColumns.NEEDS_SYNC, false);

    return values;
  }
}
