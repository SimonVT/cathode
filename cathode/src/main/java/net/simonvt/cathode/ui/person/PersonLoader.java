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

package net.simonvt.cathode.ui.person;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.database.BaseAsyncLoader;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCrewColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCrew;
import net.simonvt.cathode.provider.ProviderSchematic.People;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCast;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCrew;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.schematic.Cursors;

public class PersonLoader extends BaseAsyncLoader<Person> {

  private static final String[] SHOW_CAST_PROJECTION = new String[] {
      SqlColumn.table(Tables.SHOW_CAST).column(ShowCastColumns.SHOW_ID),
      SqlColumn.table(Tables.SHOW_CAST).column(ShowCastColumns.CHARACTER),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.POSTER),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.YEAR),
  };

  private static final String[] SHOW_CREW_PROJECTION = new String[] {
      SqlColumn.table(Tables.SHOW_CREW).column(ShowCrewColumns.SHOW_ID),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.POSTER),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.YEAR),
  };

  private static final String[] MOVIE_CAST_PROJECTION = new String[] {
      SqlColumn.table(Tables.MOVIE_CAST).column(MovieCastColumns.MOVIE_ID),
      SqlColumn.table(Tables.MOVIE_CAST).column(MovieCastColumns.CHARACTER),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.POSTER),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.YEAR),
  };

  private static final String[] MOVIE_CREW_PROJECTION = new String[] {
      SqlColumn.table(Tables.MOVIE_CREW).column(MovieCrewColumns.MOVIE_ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.POSTER),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.YEAR),
  };

  private long personId;

  private ContentResolver resolver;

  public PersonLoader(Context context, long personId) {
    super(context);
    this.personId = personId;
    resolver = context.getContentResolver();

    addNotificationUri(People.withId(personId));
    addNotificationUri(ShowCast.withPerson(personId));
    addNotificationUri(ShowCrew.withPerson(personId));
    addNotificationUri(MovieCast.withPerson(personId));
    addNotificationUri(MovieCrew.withPerson(personId));
  }

  @Override public Person loadInBackground() {
    Cursor person = resolver.query(People.withId(personId), new String[] {
        PersonColumns.NAME, PersonColumns.HEADSHOT, PersonColumns.FANART, PersonColumns.BIOGRAPHY,
        PersonColumns.BIRTHDAY, PersonColumns.DEATH, PersonColumns.BIRTHPLACE,
        PersonColumns.HOMEPAGE, PersonColumns.LAST_SYNC,
    }, null, null, null);
    try {
      if (person.moveToFirst()) {
        final String name = Cursors.getString(person, PersonColumns.NAME);
        final String headshot = Cursors.getString(person, PersonColumns.HEADSHOT);
        final String fanart = Cursors.getString(person, PersonColumns.FANART);
        final String biography = Cursors.getString(person, PersonColumns.BIOGRAPHY);
        final String birthday = Cursors.getString(person, PersonColumns.BIRTHDAY);
        final String death = Cursors.getString(person, PersonColumns.DEATH);
        final String birthplace = Cursors.getString(person, PersonColumns.BIRTHPLACE);
        final String homepage = Cursors.getString(person, PersonColumns.HOMEPAGE);
        final long lastSync = Cursors.getLong(person, PersonColumns.LAST_SYNC);

        PersonCredits credits = new PersonCredits();
        loadShowCredits(credits);
        loadMovieCredits(credits);

        Collections.sort(credits.getCast(), comparator);
        Collections.sort(credits.getProduction(), comparator);
        Collections.sort(credits.getArt(), comparator);
        Collections.sort(credits.getCrew(), comparator);
        Collections.sort(credits.getCostumeAndMakeUp(), comparator);
        Collections.sort(credits.getDirecting(), comparator);
        Collections.sort(credits.getWriting(), comparator);
        Collections.sort(credits.getSound(), comparator);
        Collections.sort(credits.getCamera(), comparator);

        return new Person(name, headshot, fanart, biography, birthday, death, birthplace, homepage,
            lastSync, credits);
      }
    } finally {
      person.close();
    }

    throw new RuntimeException("Person not found");
  }

  private Comparator<PersonCredit> comparator = new Comparator<PersonCredit>() {
    @Override public int compare(PersonCredit credit1, PersonCredit credit2) {
      return credit2.getYear() - credit1.getYear();
    }
  };

  private void loadShowCredits(PersonCredits credits) {
    Uri castUri = ShowCast.withPerson(personId);
    Uri crewUri = ShowCrew.withPerson(personId);

    List<PersonCredit> cast = null;
    Cursor castCursor = resolver.query(castUri, SHOW_CAST_PROJECTION, null, null, null);

    if (castCursor.getCount() > 0) {
      cast = new ArrayList<>();

      while (castCursor.moveToNext()) {
        final String character = Cursors.getString(castCursor, ShowCastColumns.CHARACTER);
        final long itemId = Cursors.getLong(castCursor, ShowCastColumns.SHOW_ID);
        final String poster = Cursors.getString(castCursor, ShowColumns.POSTER);
        final String title = Cursors.getString(castCursor, ShowColumns.TITLE);
        final String overview = Cursors.getString(castCursor, ShowColumns.OVERVIEW);
        final int year = Cursors.getInt(castCursor, ShowColumns.YEAR);

        PersonCredit credit =
            PersonCredit.character(character, ItemType.SHOW, itemId, poster, title, overview, year);
        cast.add(credit);
      }
    }

    castCursor.close();

    if (cast != null) {
      credits.addCast(cast);
    }

    credits.addProduction(parseShowCrew(crewUri, Department.PRODUCTION));
    credits.addArt(parseShowCrew(crewUri, Department.ART));
    credits.addCrew(parseShowCrew(crewUri, Department.CREW));
    credits.addCostumeAndMakeUp(parseShowCrew(crewUri, Department.COSTUME_AND_MAKEUP));
    credits.addDirecting(parseShowCrew(crewUri, Department.DIRECTING));
    credits.addWriting(parseShowCrew(crewUri, Department.WRITING));
    credits.addSound(parseShowCrew(crewUri, Department.SOUND));
    credits.addCamera(parseShowCrew(crewUri, Department.CAMERA));
  }

  private List<PersonCredit> parseShowCrew(Uri uri, Department department) {
    Cursor cursor =
        resolver.query(uri, SHOW_CREW_PROJECTION, ShowCrewColumns.CATEGORY + "=?", new String[] {
            department.toString(),
        }, null);

    try {
      if (cursor.getCount() > 0) {
        List<PersonCredit> credits = new ArrayList<>();

        while (cursor.moveToNext()) {
          final long itemId = Cursors.getLong(cursor, ShowCrewColumns.SHOW_ID);
          final String poster = Cursors.getString(cursor, ShowColumns.POSTER);
          final String title = Cursors.getString(cursor, ShowColumns.TITLE);
          final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
          final int year = Cursors.getInt(cursor, ShowColumns.YEAR);

          PersonCredit credit =
              PersonCredit.job(department.toString(), ItemType.SHOW, itemId, poster, title,
                  overview, year);
          credits.add(credit);
        }

        return credits;
      }

      return null;
    } finally {
      cursor.close();
    }
  }

  private void loadMovieCredits(PersonCredits credits) {
    Uri castUri = MovieCast.withPerson(personId);
    Uri crewUri = MovieCrew.withPerson(personId);

    List<PersonCredit> cast = null;
    Cursor castCursor = resolver.query(castUri, MOVIE_CAST_PROJECTION, null, null, null);

    if (castCursor.getCount() > 0) {
      cast = new ArrayList<>();

      while (castCursor.moveToNext()) {
        final String character = Cursors.getString(castCursor, MovieCastColumns.CHARACTER);
        final long itemId = Cursors.getLong(castCursor, MovieCastColumns.MOVIE_ID);
        final String poster = Cursors.getString(castCursor, MovieColumns.POSTER);
        final String title = Cursors.getString(castCursor, MovieColumns.TITLE);
        final String overview = Cursors.getString(castCursor, MovieColumns.OVERVIEW);
        final int year = Cursors.getInt(castCursor, MovieColumns.YEAR);

        PersonCredit credit =
            PersonCredit.character(character, ItemType.MOVIE, itemId, poster, title, overview,
                year);
        cast.add(credit);
      }
    }

    castCursor.close();

    if (cast != null) {
      credits.addCast(cast);
    }

    credits.addProduction(parseMovieCrew(crewUri, Department.PRODUCTION));
    credits.addArt(parseMovieCrew(crewUri, Department.ART));
    credits.addCrew(parseMovieCrew(crewUri, Department.CREW));
    credits.addCostumeAndMakeUp(parseMovieCrew(crewUri, Department.COSTUME_AND_MAKEUP));
    credits.addDirecting(parseMovieCrew(crewUri, Department.DIRECTING));
    credits.addWriting(parseMovieCrew(crewUri, Department.WRITING));
    credits.addSound(parseMovieCrew(crewUri, Department.SOUND));
    credits.addCamera(parseMovieCrew(crewUri, Department.CAMERA));
  }

  private List<PersonCredit> parseMovieCrew(Uri uri, Department department) {
    Cursor cursor =
        resolver.query(uri, MOVIE_CREW_PROJECTION, MovieCrewColumns.CATEGORY + "=?", new String[] {
            department.toString(),
        }, null);

    try {
      if (cursor.getCount() > 0) {
        List<PersonCredit> credits = new ArrayList<>();

        while (cursor.moveToNext()) {
          final long itemId = Cursors.getLong(cursor, MovieCrewColumns.MOVIE_ID);
          final String poster = Cursors.getString(cursor, MovieColumns.POSTER);
          final String title = Cursors.getString(cursor, MovieColumns.TITLE);
          final String overview = Cursors.getString(cursor, MovieColumns.OVERVIEW);
          final int year = Cursors.getInt(cursor, MovieColumns.YEAR);

          PersonCredit credit =
              PersonCredit.job(department.toString(), ItemType.MOVIE, itemId, poster, title,
                  overview, year);
          credits.add(credit);
        }

        return credits;
      }

      return null;
    } finally {
      cursor.close();
    }
  }
}
