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
package net.simonvt.cathode.provider.helper

import android.content.ContentValues
import android.content.Context
import net.simonvt.cathode.provider.DatabaseContract.RecentQueriesColumns
import net.simonvt.cathode.provider.ProviderSchematic.RecentQueries
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchDatabaseHelper @Inject constructor(private val context: Context) {

  fun insertRecentQuery(query: String) {
    context.contentResolver.delete(
      RecentQueries.RECENT_QUERIES,
      RecentQueriesColumns.QUERY + "=?",
      arrayOf(query)
    )

    val values = ContentValues()
    values.put(RecentQueriesColumns.QUERY, query)
    values.put(RecentQueriesColumns.QUERIED_AT, System.currentTimeMillis())
    context.contentResolver.insert(RecentQueries.RECENT_QUERIES, values)
  }
}
