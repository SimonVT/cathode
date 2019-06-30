/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import net.simonvt.cathode.common.database.SimpleCursor
import timber.log.Timber

abstract class BaseCursorLiveData<D>(
  context: Context,
  private val uri: Uri,
  private val projection: Array<String>,
  private val selection: String? = null,
  private val selectionArgs: Array<String>? = null,
  private var sortOrder: String? = null
) : ListenableLiveData<D>(context), ThrottleContentObserver.Callback {

  private var notificationUri: Uri? = null

  fun setSortOrder(sortOrder: String) {
    this.sortOrder = sortOrder
    loadData()
  }

  override fun onContentChanged() {
    loadData()
  }

  fun loadCursor(): Cursor? {
    try {
      val cursor =
        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
      var result: SimpleCursor? = null
      if (cursor != null) {
        val oldNotificationUri = notificationUri
        notificationUri = cursor.notificationUri
        if (oldNotificationUri == null) {
          registerUri(notificationUri!!)
        } else if (oldNotificationUri != notificationUri) {
          unregisterUri(oldNotificationUri)
          registerUri(notificationUri!!)
        }

        result = SimpleCursor(cursor)
        cursor.close()
      }
      return result
    } catch (e: SQLiteException) {
      Timber.e(e, "Query failed")
    }

    return null
  }
}
