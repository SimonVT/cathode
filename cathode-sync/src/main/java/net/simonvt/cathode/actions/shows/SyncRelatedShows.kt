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
package net.simonvt.cathode.actions.shows

import android.content.ContentProviderOperation
import android.content.Context
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.shows.SyncRelatedShows.Params
import net.simonvt.cathode.api.entity.Show
import net.simonvt.cathode.api.enumeration.Extended
import net.simonvt.cathode.api.service.ShowsService
import net.simonvt.cathode.common.database.forEach
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.RelatedShowsColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.provider.query
import retrofit2.Call
import javax.inject.Inject

class SyncRelatedShows @Inject constructor(
  private val context: Context,
  private val showsService: ShowsService,
  private val showHelper: ShowDatabaseHelper
) : CallAction<Params, List<Show>>() {

  override fun getCall(params: Params): Call<List<Show>> =
    showsService.getRelated(params.traktId, RELATED_COUNT, Extended.FULL)

  override suspend fun handleResponse(params: Params, response: List<Show>) {
    val showId = showHelper.getId(params.traktId)

    val ops = arrayListOf<ContentProviderOperation>()
    val relatedIds = mutableListOf<Long>()

    val related = context.contentResolver.query(
      RelatedShows.fromShow(showId),
      arrayOf(Tables.SHOW_RELATED + "." + RelatedShowsColumns.ID)
    )
    related.forEach { cursor -> relatedIds.add(cursor.getLong(RelatedShowsColumns.ID)) }
    related.close()

    for ((index, show) in response.withIndex()) {
      val relatedShowId = showHelper.partialUpdate(show)

      val op = ContentProviderOperation.newInsert(RelatedShows.RELATED)
        .withValue(RelatedShowsColumns.SHOW_ID, showId)
        .withValue(RelatedShowsColumns.RELATED_SHOW_ID, relatedShowId)
        .withValue(RelatedShowsColumns.RELATED_INDEX, index)
        .build()
      ops.add(op)
    }

    for (id in relatedIds) {
      ops.add(ContentProviderOperation.newDelete(RelatedShows.withId(id)).build())
    }

    ops.add(
      ContentProviderOperation.newUpdate(Shows.withId(showId))
        .withValue(ShowColumns.LAST_RELATED_SYNC, System.currentTimeMillis())
        .build()
    )

    context.contentResolver.batch(ops)
  }

  data class Params(val traktId: Long)

  companion object {

    private const val RELATED_COUNT = 50

    fun key(traktId: Long): String {
      return "SyncRelatedShows&traktId=$traktId"
    }
  }
}
