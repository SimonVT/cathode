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
import android.content.Context;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.provider.ProviderSchematic.RecentQueries;

public final class SearchDatabaseHelper {

  private static volatile SearchDatabaseHelper instance;

  public static SearchDatabaseHelper getInstance(Context context) {
    if (instance == null) {
      synchronized (SearchDatabaseHelper.class) {
        if (instance == null) {
          instance = new SearchDatabaseHelper(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  private Context context;

  private ContentResolver resolver;

  private SearchDatabaseHelper(Context context) {
    this.context = context;

    resolver = context.getContentResolver();

    Injector.inject(this);
  }

  public void insertRecentQuery(String query) {
    resolver.delete(RecentQueries.RECENT_QUERIES,
        DatabaseContract.RecentQueriesColumns.QUERY + "=?", new String[] {
            query,
        });

    final ContentValues values = new ContentValues();
    values.put(DatabaseContract.RecentQueriesColumns.QUERY, query);
    values.put(DatabaseContract.RecentQueriesColumns.QUERIED_AT, System.currentTimeMillis());
    resolver.insert(RecentQueries.RECENT_QUERIES, values);
  }
}
