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

package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.IsoTime;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.People;

public final class PersonWrapper {

  private PersonWrapper() {
  }

  public static long getId(ContentResolver resolver, long traktId) {
    Cursor c = resolver.query(People.PEOPLE, new String[] {
        PersonColumns.ID,
    }, PersonColumns.TRAKT_ID + "=?", new String[] {
        String.valueOf(traktId),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(PersonColumns.ID));

    c.close();

    return id;
  }

  public static long createPerson(ContentResolver resolver, long traktId) {
    ContentValues values = new ContentValues();
    values.put(PersonColumns.TRAKT_ID, traktId);
    values.put(PersonColumns.NEEDS_SYNC, true);

    return People.getId(resolver.insert(People.PEOPLE, values));
  }

  public static long updateOrInsert(ContentResolver resolver, Person person) {
    final long traktId = person.getIds().getTrakt();
    long personId = getId(resolver, traktId);

    if (personId == -1L) {
      personId = People.getId(resolver.insert(People.PEOPLE, getValues(person)));
    } else {
      resolver.update(People.withId(personId), getValues(person), null, null);
    }

    return personId;
  }

  private static ContentValues getValues(Person person) {
    ContentValues cv = new ContentValues();

    cv.put(PersonColumns.NAME, person.getName());
    cv.put(PersonColumns.TRAKT_ID, person.getIds().getTrakt());
    cv.put(PersonColumns.SLUG, person.getIds().getSlug());
    cv.put(PersonColumns.IMDB_ID, person.getIds().getImdb());
    cv.put(PersonColumns.TMDB_ID, person.getIds().getTmdb());
    cv.put(PersonColumns.TVRAGE_ID, person.getIds().getTvrage());

    Images images = person.getImages();
    if (images != null && images.getHeadshot() != null) {
      cv.put(PersonColumns.HEADSHOT, images.getHeadshot().getFull());
    } else {
      cv.putNull(PersonColumns.HEADSHOT);
    }

    cv.put(PersonColumns.BIOGRAPHY, person.getBiography());
    IsoTime death = person.getDeath();
    if (death != null) {
      cv.put(PersonColumns.DEATH, death.getTime());
    } else {
      cv.putNull(PersonColumns.DEATH);
    }
    cv.put(PersonColumns.BIRTHPLACE, person.getBirthplace());
    cv.put(PersonColumns.HOMEPAGE, person.getHomepage());

    cv.put(PersonColumns.NEEDS_SYNC, false);

    return cv;
  }
}
