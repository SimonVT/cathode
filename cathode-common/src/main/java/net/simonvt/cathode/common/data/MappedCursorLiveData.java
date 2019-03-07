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

package net.simonvt.cathode.common.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.Nullable;

public class MappedCursorLiveData<D> extends BaseCursorLiveData<D> {

  public interface CursorMapper<D> {

    D map(Cursor cursor);
  }

  private CursorMapper<D> mapper;

  public MappedCursorLiveData(Context context, Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder, CursorMapper<D> mapper) {
    super(context, uri, projection, selection, selectionArgs, sortOrder);
    this.mapper = mapper;
  }

  @Nullable @Override protected D loadInBackground() {
    Cursor cursor = loadCursor();
    if (cursor != null) {
      return mapper.map(cursor);
    }
    return null;
  }
}
