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

package net.simonvt.cathode.provider.helper

import android.content.ContentValues
import android.content.Context
import net.simonvt.cathode.api.entity.CustomList
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns
import net.simonvt.cathode.provider.ProviderSchematic.Lists
import net.simonvt.cathode.provider.query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListDatabaseHelper @Inject constructor(
  private val context: Context
) {

  fun getId(traktId: Long): Long {
    val c = context.contentResolver.query(
      Lists.LISTS,
      arrayOf(ListsColumns.ID),
      ListsColumns.TRAKT_ID + "=?",
      arrayOf(traktId.toString())
    )
    val id = if (!c.moveToFirst()) -1L else c.getLong(ListsColumns.ID)
    c.close()
    return id
  }

  fun getTraktId(listId: Long): Long {
    val c = context.contentResolver.query(Lists.withId(listId), arrayOf(ListsColumns.TRAKT_ID))
    val traktId = if (!c.moveToFirst()) -1L else c.getLong(ListsColumns.TRAKT_ID)
    c.close()
    return traktId
  }

  fun updateOrInsert(list: CustomList): Long {
    val traktId = list.ids.trakt!!
    var listId = getId(traktId)

    if (listId == -1L) {
      listId = Lists.getId(context.contentResolver.insert(Lists.LISTS, getValues(list))!!)
    } else {
      update(listId, list)
    }

    return listId
  }

  fun update(listId: Long, list: CustomList) {
    context.contentResolver.update(Lists.withId(listId), getValues(list), null, null)
  }

  private fun getValues(list: CustomList): ContentValues {
    val values = ContentValues()

    values.put(ListsColumns.NAME, list.name)
    values.put(ListsColumns.DESCRIPTION, list.description)
    values.put(ListsColumns.PRIVACY, list.privacy.toString())
    values.put(ListsColumns.DISPLAY_NUMBERS, list.display_numbers)
    values.put(ListsColumns.ALLOW_COMMENTS, list.allow_comments)
    values.put(ListsColumns.SORT_BY, list.sort_by.toString())
    values.put(ListsColumns.SORT_ORIENTATION, list.sort_how.toString())
    values.put(ListsColumns.UPDATED_AT, list.updated_at?.timeInMillis)
    values.put(ListsColumns.LIKES, list.likes)
    values.put(ListsColumns.SLUG, list.ids.slug)
    values.put(ListsColumns.TRAKT_ID, list.ids.trakt)

    return values
  }
}
