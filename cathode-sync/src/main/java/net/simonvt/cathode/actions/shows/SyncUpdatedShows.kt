/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import android.text.format.DateUtils
import net.simonvt.cathode.actions.PagedAction
import net.simonvt.cathode.actions.PagedResponse
import net.simonvt.cathode.api.entity.UpdatedItem
import net.simonvt.cathode.api.service.ShowsService
import net.simonvt.cathode.api.util.TimeUtils
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.batch
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.settings.Timestamps
import retrofit2.Call
import javax.inject.Inject

class SyncUpdatedShows @Inject constructor(
  private val context: Context,
  private val showsService: ShowsService,
  private val showHelper: ShowDatabaseHelper
) : PagedAction<Unit, UpdatedItem>() {

  override fun key(params: Unit): String = "SyncUpdatedShows"

  private val currentTime = System.currentTimeMillis()

  override fun getCall(params: Unit, page: Int): Call<List<UpdatedItem>> {
    val lastUpdated =
      Timestamps.get(context).getLong(Timestamps.SHOWS_LAST_UPDATED, currentTime)
    val millis = lastUpdated - 12 * DateUtils.HOUR_IN_MILLIS
    val updatedSince = TimeUtils.getIsoTime(millis)
    return showsService.getUpdatedShows(updatedSince, page, LIMIT)
  }

  override suspend fun handleResponse(
    params: Unit,
    pagedResponse: PagedResponse<Unit, UpdatedItem>
  ) {
    val ops = arrayListOf<ContentProviderOperation>()

    var page: PagedResponse<Unit, UpdatedItem>? = pagedResponse
    do {
      for (item in page!!.response) {
        val updatedAt = item.updated_at.timeInMillis
        val show = item.show!!
        val traktId = show.ids.trakt!!
        val id = showHelper.getId(traktId)
        if (id != -1L) {
          if (showHelper.isUpdated(traktId, updatedAt)) {
            val values = ContentValues()
            values.put(ShowColumns.NEEDS_SYNC, true)
            ops.add(ContentProviderOperation.newUpdate(Shows.withId(id)).withValues(values).build())
          }
        }
      }

      page = page.nextPage()
    } while (page != null)

    context.contentResolver.batch(ops)
  }

  override fun onDone() {
    Timestamps.get(context).edit().putLong(Timestamps.SHOWS_LAST_UPDATED, currentTime).apply()
  }

  companion object {
    private const val LIMIT = 100
  }
}
