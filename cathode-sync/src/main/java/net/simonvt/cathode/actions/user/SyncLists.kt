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

package net.simonvt.cathode.actions.user

import android.content.Context
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.CallAction
import net.simonvt.cathode.actions.lists.SyncList
import net.simonvt.cathode.api.entity.CustomList
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns
import net.simonvt.cathode.provider.ProviderSchematic.Lists
import net.simonvt.cathode.provider.helper.ListWrapper
import retrofit2.Call
import javax.inject.Inject

class SyncLists @Inject constructor(
  private val context: Context,
  private val usersService: UsersService,
  private val syncList: SyncList
) : CallAction<Unit, List<CustomList>>() {

  override fun getCall(params: Unit): Call<List<CustomList>> = usersService.lists()

  override suspend fun handleResponse(params: Unit, response: List<CustomList>) {
    val listIds = mutableListOf<Long>()
    val listsCursor = context.contentResolver.query(Lists.LISTS, PROJECTION, null, null, null)
    while (listsCursor!!.moveToNext()) {
      listIds.add(Cursors.getLong(listsCursor, ListsColumns.ID))
    }
    listsCursor.close()

    response.map { list ->
      val traktId = list.ids.trakt!!
      val listId = ListWrapper.updateOrInsert(context.contentResolver, list)
      listIds.remove(listId)
      ActionManager.invokeAsync(SyncList.key(traktId), syncList, SyncList.Params(traktId))
    }.map { it.await() }

    for (id in listIds) {
      context.contentResolver.delete(Lists.withId(id), null, null)
    }
  }

  companion object {

    private val PROJECTION = arrayOf(ListsColumns.ID, ListsColumns.TRAKT_ID)

    fun key() = "SyncLists"
  }
}
