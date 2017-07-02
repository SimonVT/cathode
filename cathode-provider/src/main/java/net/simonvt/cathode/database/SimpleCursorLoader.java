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
package net.simonvt.cathode.database;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class SimpleCursorLoader extends BaseAsyncLoader<SimpleCursor> {

  private Uri uri;
  private String[] projection;
  private String selection;
  private String[] selectionArgs;
  private String sortOrder;

  private Uri notificationUri;

  public SimpleCursorLoader(Context context, Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    super(context);
    this.uri = uri;
    this.projection = projection;
    this.selection = selection;
    this.selectionArgs = selectionArgs;
    this.sortOrder = sortOrder;
  }

  @Override public SimpleCursor loadInBackground() {
    Cursor cursor = getContext().getContentResolver()
        .query(uri, projection, selection, selectionArgs, sortOrder);
    SimpleCursor result = null;
    if (cursor != null) {
      clearNotificationUris();
      notificationUri = DatabaseUtils.getNotificationUri(cursor);
      addNotificationUri(notificationUri);

      result = new SimpleCursor(cursor);
      cursor.close();
    }
    return result;
  }
}
