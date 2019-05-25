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
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import net.simonvt.cathode.common.database.SimpleCursor;
import timber.log.Timber;

abstract class BaseCursorLiveData<D> extends ListenableLiveData<D>
    implements ThrottleContentObserver.Callback {

  private Context context;

  private Uri uri;
  private String[] projection;
  private String selection;
  private String[] selectionArgs;
  private String sortOrder;

  private Uri notificationUri;

  public BaseCursorLiveData(Context context, Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    super(context);
    this.context = context;
    this.uri = uri;
    this.projection = projection;
    this.selection = selection;
    this.selectionArgs = selectionArgs;
    this.sortOrder = sortOrder;
  }

  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
    loadData();
  }

  @Override public void onContentChanged() {
    loadData();
  }

  public Cursor loadCursor() {
    try {
      Cursor cursor =
          context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
      SimpleCursor result = null;
      if (cursor != null) {
        Uri oldNotificationUri = notificationUri;
        notificationUri = cursor.getNotificationUri();
        if (oldNotificationUri == null) {
          registerUri(notificationUri);
        } else if (!oldNotificationUri.equals(notificationUri)) {
          unregisterUri(oldNotificationUri);
          registerUri(notificationUri);
        }

        result = new SimpleCursor(cursor);
        cursor.close();
      }
      return result;
    } catch (SQLiteException e) {
      Timber.e(e, "Query failed");
    }

    return null;
  }
}
