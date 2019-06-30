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
import android.net.Uri

class MappedCursorLiveData<D>(
  context: Context,
  uri: Uri,
  projection: Array<String>,
  selection: String? = null,
  selectionArgs: Array<String>? = null,
  sortOrder: String? = null,
  private val mapper: CursorMapper<D>,
  private val allowNulls: Boolean = false
) : BaseCursorLiveData<D>(context, uri, projection, selection, selectionArgs, sortOrder) {

  interface CursorMapper<D> {

    fun map(cursor: Cursor): D?
  }

  override fun loadInBackground(): D? {
    val cursor = loadCursor()
    return if (cursor != null) mapper.map(cursor) else null
  }

  override fun setValue(value: D) {
    if (allowNulls || value != null) {
      super.setValue(value)
    }
  }
}
