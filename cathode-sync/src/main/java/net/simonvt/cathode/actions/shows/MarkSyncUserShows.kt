/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.actions.shows

import android.content.ContentValues
import android.content.Context
import android.text.format.DateUtils
import net.simonvt.cathode.actions.ErrorHandlerAction
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import javax.inject.Inject

class MarkSyncUserShows @Inject constructor(
  private val context: Context
) : ErrorHandlerAction<Unit>() {

  override fun key(params: Unit): String = "MarkSyncUserShows"

  override suspend fun invoke(params: Unit) {
    val syncBefore = System.currentTimeMillis() - SYNC_INTERVAL
    val values = ContentValues()
    values.put(ShowColumns.NEEDS_SYNC, true)
    context.contentResolver.update(
      Shows.SHOWS, values, "(" +
          ShowColumns.WATCHED_COUNT + ">0 OR " +
          ShowColumns.IN_COLLECTION_COUNT + ">0 OR " +
          ShowColumns.IN_WATCHLIST_COUNT + ">0 OR " +
          ShowColumns.IN_WATCHLIST + ") AND " +
          ShowColumns.LAST_SYNC + "<?",
      arrayOf(syncBefore.toString())
    )
  }

  companion object {
    const val SYNC_INTERVAL = 30 * DateUtils.DAY_IN_MILLIS
  }
}
