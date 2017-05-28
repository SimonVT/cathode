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

package net.simonvt.cathode.ui.credits;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.api.enumeration.Department;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.database.BaseAsyncLoader;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCrewColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCrewColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCrew;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCast;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCrew;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.schematic.Cursors;

public class CreditsLoader extends BaseAsyncLoader<Credits> {

  private static final String[] SHOW_CAST_PROJECTION = new String[] {
      SqlColumn.table(Tables.SHOW_CAST).column(ShowCastColumns.SHOW_ID),
      SqlColumn.table(Tables.SHOW_CAST).column(ShowCastColumns.CHARACTER),
      SqlColumn.table(Tables.SHOW_CAST).column(ShowCastColumns.PERSON_ID),
      SqlColumn.table(Tables.PEOPLE).column(PersonColumns.NAME),
  };

  private static final String[] SHOW_CREW_PROJECTION = new String[] {
      SqlColumn.table(Tables.SHOW_CREW).column(ShowCrewColumns.SHOW_ID),
      SqlColumn.table(Tables.SHOW_CREW).column(ShowCrewColumns.JOB),
      SqlColumn.table(Tables.SHOW_CREW).column(ShowCrewColumns.PERSON_ID),
      SqlColumn.table(Tables.PEOPLE).column(PersonColumns.NAME),
  };

  private static final String[] MOVIE_CAST_PROJECTION = new String[] {
      SqlColumn.table(Tables.MOVIE_CAST).column(MovieCastColumns.MOVIE_ID),
      SqlColumn.table(Tables.MOVIE_CAST).column(MovieCastColumns.CHARACTER),
      SqlColumn.table(Tables.MOVIE_CAST).column(MovieCastColumns.PERSON_ID),
      SqlColumn.table(Tables.PEOPLE).column(PersonColumns.NAME),
  };

  private static final String[] MOVIE_CREW_PROJECTION = new String[] {
      SqlColumn.table(Tables.MOVIE_CREW).column(MovieCrewColumns.MOVIE_ID),
      SqlColumn.table(Tables.MOVIE_CREW).column(MovieCrewColumns.JOB),
      SqlColumn.table(Tables.MOVIE_CREW).column(MovieCrewColumns.PERSON_ID),
      SqlColumn.table(Tables.PEOPLE).column(PersonColumns.NAME),
  };

  private ItemType itemType;

  private long itemId;

  private ContentResolver resolver;

  public CreditsLoader(Context context, ItemType itemType, long itemId) {
    super(context);
    this.itemType = itemType;
    this.itemId = itemId;
    resolver = context.getContentResolver();

    if (itemType == ItemType.SHOW) {
      addNotificationUri(ShowCast.fromShow(itemId));
      addNotificationUri(ShowCrew.fromShow(itemId));
    } else {
      addNotificationUri(MovieCast.fromMovie(itemId));
      addNotificationUri(MovieCrew.fromMovie(itemId));
    }
  }

  @Override public Credits loadInBackground() {
    if (itemType == ItemType.SHOW) {
      return loadShowCredits();
    } else {
      return loadMovieCredits();
    }
  }

  private Credits loadShowCredits() {
    Uri castUri = ShowCast.fromShow(itemId);
    Uri crewUri = ShowCrew.fromShow(itemId);

    List<Credit> cast = null;
    Cursor castCursor = resolver.query(castUri, SHOW_CAST_PROJECTION, null, null, null);

    if (castCursor.getCount() > 0) {
      cast = new ArrayList<>();

      while (castCursor.moveToNext()) {
        final String character = Cursors.getString(castCursor, ShowCastColumns.CHARACTER);
        final long personId = Cursors.getLong(castCursor, ShowCastColumns.PERSON_ID);
        final String name = Cursors.getString(castCursor, PersonColumns.NAME);

        final String headshot =
            ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE,
                personId);

        Credit credit = Credit.character(character, personId, name, headshot);
        cast.add(credit);
      }
    }

    castCursor.close();

    List<Credit> production = parseShowCrew(crewUri, Department.PRODUCTION);
    List<Credit> art = parseShowCrew(crewUri, Department.ART);
    List<Credit> crew = parseShowCrew(crewUri, Department.CREW);
    List<Credit> costumeAndMakeUp = parseShowCrew(crewUri, Department.COSTUME_AND_MAKEUP);
    List<Credit> directing = parseShowCrew(crewUri, Department.DIRECTING);
    List<Credit> writing = parseShowCrew(crewUri, Department.WRITING);
    List<Credit> sound = parseShowCrew(crewUri, Department.SOUND);
    List<Credit> camera = parseShowCrew(crewUri, Department.CAMERA);

    Credits credits =
        new Credits(cast, production, art, crew, costumeAndMakeUp, directing, writing, sound,
            camera);
    return credits;
  }

  private List<Credit> parseShowCrew(Uri uri, Department department) {
    Cursor cursor =
        resolver.query(uri, SHOW_CREW_PROJECTION, ShowCrewColumns.CATEGORY + "=?", new String[] {
            department.toString(),
        }, null);

    try {
      if (cursor.getCount() > 0) {
        List<Credit> credits = new ArrayList<>();

        while (cursor.moveToNext()) {
          final String character = Cursors.getString(cursor, ShowCrewColumns.JOB);
          final long personId = Cursors.getLong(cursor, ShowCrewColumns.PERSON_ID);
          final String name = Cursors.getString(cursor, PersonColumns.NAME);

          final String headshot =
              ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE,
                  personId);

          Credit credit = Credit.character(character, personId, name, headshot);
          credits.add(credit);
        }

        return credits;
      }

      return null;
    } finally {
      cursor.close();
    }
  }

  private Credits loadMovieCredits() {
    Uri castUri = MovieCast.fromMovie(itemId);
    Uri crewUri = MovieCrew.fromMovie(itemId);

    List<Credit> cast = null;
    Cursor castCursor = resolver.query(castUri, MOVIE_CAST_PROJECTION, null, null, null);

    if (castCursor.getCount() > 0) {
      cast = new ArrayList<>();

      while (castCursor.moveToNext()) {
        final String character = Cursors.getString(castCursor, MovieCastColumns.CHARACTER);
        final long personId = Cursors.getLong(castCursor, MovieCastColumns.PERSON_ID);
        final String name = Cursors.getString(castCursor, PersonColumns.NAME);

        final String headshot =
            ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE,
                personId);

        Credit credit = Credit.character(character, personId, name, headshot);
        cast.add(credit);
      }
    }

    castCursor.close();

    List<Credit> production = parseMovieCrew(crewUri, Department.PRODUCTION);
    List<Credit> art = parseMovieCrew(crewUri, Department.ART);
    List<Credit> crew = parseMovieCrew(crewUri, Department.CREW);
    List<Credit> costumeAndMakeUp = parseMovieCrew(crewUri, Department.COSTUME_AND_MAKEUP);
    List<Credit> directing = parseMovieCrew(crewUri, Department.DIRECTING);
    List<Credit> writing = parseMovieCrew(crewUri, Department.WRITING);
    List<Credit> sound = parseMovieCrew(crewUri, Department.SOUND);
    List<Credit> camera = parseMovieCrew(crewUri, Department.CAMERA);

    Credits credits =
        new Credits(cast, production, art, crew, costumeAndMakeUp, directing, writing, sound,
            camera);
    return credits;
  }

  private List<Credit> parseMovieCrew(Uri uri, Department department) {
    Cursor cursor =
        resolver.query(uri, MOVIE_CREW_PROJECTION, MovieCrewColumns.CATEGORY + "=?", new String[] {
            department.toString(),
        }, null);

    try {
      if (cursor.getCount() > 0) {
        List<Credit> credits = new ArrayList<>();

        while (cursor.moveToNext()) {
          final String character = Cursors.getString(cursor, MovieCrewColumns.JOB);
          final long personId = Cursors.getLong(cursor, MovieCrewColumns.PERSON_ID);
          final String name = Cursors.getString(cursor, PersonColumns.NAME);

          final String headshot =
              ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE,
                  personId);

          Credit credit = Credit.character(character, personId, name, headshot);
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
