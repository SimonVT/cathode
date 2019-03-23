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
package net.simonvt.cathode.actions.shows

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.api.entity.TrendingItem
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.ShowsService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.settings.SuggestionsTimestamps
import retrofit2.Call
import javax.inject.Inject

class SyncTrendingShows @Inject constructor(
  private val context: Context,
  private val showHelper: ShowDatabaseHelper,
  private val showsService: ShowsService
) : CallAction<Unit, List<TrendingItem>>() {

  override fun getCall(params: Unit): Call<List<TrendingItem>> =
    showsService.getTrendingShows(LIMIT, Extended.FULL)

  override suspend fun handleResponse(params: Unit, response: List<TrendingItem>) {
    val ops = arrayListOf<ContentProviderOperation>()
    val showIds = mutableListOf<Long>()

    val localShows = context.contentResolver.query(Shows.SHOWS_TRENDING)
    localShows.forEach { cursor -> showIds.add(cursor.getLong(ShowColumns.ID)) }
    localShows.close()

    response.forEachIndexed { index, trendingItem ->
      val show = trendingItem.show!!
      val showId = showHelper.partialUpdate(show)
      showIds.remove(showId)

      val values = ContentValues()
      values.put(ShowColumns.TRENDING_INDEX, index)
      val op = ContentProviderOperation.newUpdate(Shows.withId(showId)).withValues(values).build()
      ops.add(op)
    }

    for (showId in showIds) {
      val op = ContentProviderOperation.newUpdate(Shows.withId(showId))
        .withValue(ShowColumns.TRENDING_INDEX, -1)
        .build()
      ops.add(op)
    }

    context.contentResolver.batch(ops)

    SuggestionsTimestamps.get(context)
      .edit()
      .putLong(SuggestionsTimestamps.SHOWS_TRENDING, System.currentTimeMillis())
      .apply()
  }

  companion object {

    private const val LIMIT = 50

    fun key() = "SyncTrendingShows"
  }
}
