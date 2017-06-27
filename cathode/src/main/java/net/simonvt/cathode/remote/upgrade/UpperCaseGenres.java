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

package net.simonvt.cathode.remote.upgrade;

import android.content.ContentValues;
import android.database.Cursor;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres;
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres;
import net.simonvt.cathode.util.TextUtils;
import net.simonvt.schematic.Cursors;

public class UpperCaseGenres extends Job {

  @Override public String key() {
    return "UpperCaseGenres";
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public boolean perform() {
    Cursor showGenres = getContentResolver().query(ShowGenres.SHOW_GENRES, new String[] {
        ShowGenreColumns.ID, ShowGenreColumns.GENRE,
    }, null, null, null);

    while (showGenres.moveToNext()) {
      final long id = Cursors.getLong(showGenres, ShowGenreColumns.ID);
      String genre = Cursors.getString(showGenres, ShowGenreColumns.GENRE);
      genre = TextUtils.upperCaseFirstLetter(genre);

      ContentValues values = new ContentValues();
      values.put(ShowGenreColumns.GENRE, genre);
      getContentResolver().update(ShowGenres.SHOW_GENRES, values, ShowGenreColumns.ID + "=?",
          new String[] {
              String.valueOf(id),
          });
    }

    showGenres.close();

    Cursor movieGenres = getContentResolver().query(MovieGenres.MOVIE_GENRES, new String[] {
        MovieGenreColumns.ID, MovieGenreColumns.GENRE,
    }, null, null, null);

    while (movieGenres.moveToNext()) {
      final long id = Cursors.getLong(movieGenres, ShowGenreColumns.ID);
      String genre = Cursors.getString(movieGenres, ShowGenreColumns.GENRE);
      genre = TextUtils.upperCaseFirstLetter(genre);

      ContentValues values = new ContentValues();
      values.put(MovieGenreColumns.GENRE, genre);
      getContentResolver().update(MovieGenres.MOVIE_GENRES, values, MovieGenreColumns.ID + "=?",
          new String[] {
              String.valueOf(id),
          });
    }

    movieGenres.close();

    return true;
  }
}
