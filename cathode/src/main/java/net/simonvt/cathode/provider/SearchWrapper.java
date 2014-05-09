/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
import net.simonvt.cathode.provider.DatabaseContract.MovieSearchSuggestionsColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowSearchSuggestionsColumns;
import net.simonvt.cathode.provider.ProviderSchematic.MovieSearchSuggestions;
import net.simonvt.cathode.provider.ProviderSchematic.ShowSearchSuggestions;

public final class SearchWrapper {

  private SearchWrapper() {
  }

  public static void insertShowQuery(ContentResolver resolver, String query) {
    Cursor c = resolver.query(ShowSearchSuggestions.SHOW_SUGGESTIONS, null,
        ShowSearchSuggestionsColumns.QUERY + "=?", new String[] {
            query,
        }, null
    );
    if (c.getCount() == 0) {
      ContentValues cv = new ContentValues();
      cv.put(ShowSearchSuggestionsColumns.QUERY, query);
      resolver.insert(ShowSearchSuggestions.SHOW_SUGGESTIONS, cv);
    }
    c.close();
  }

  public static void insertMovieQuery(ContentResolver resolver, String query) {
    Cursor c = resolver.query(MovieSearchSuggestions.MOVIE_SUGGESTIONS, null,
        MovieSearchSuggestionsColumns.QUERY + "=?", new String[] {
            query,
        }, null
    );
    if (c.getCount() == 0) {
      ContentValues cv = new ContentValues();
      cv.put(MovieSearchSuggestionsColumns.QUERY, query);
      resolver.insert(MovieSearchSuggestions.MOVIE_SUGGESTIONS, cv);
    }
    c.close();
  }
}
