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
package net.simonvt.cathode.database;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;

public final class DatabaseUtils {

  private DatabaseUtils() {
  }

  public static Uri getNotificationUri(Cursor cursor) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return cursor.getNotificationUri();
    }

    if (cursor instanceof AbstractCursor) {
      AbstractCursor ac = (AbstractCursor) cursor;
      return ac.getNotificationUri();
    }

    if (cursor instanceof CursorWrapper) {
      Cursor wrappedCursor = ((CursorWrapper) cursor).getWrappedCursor();
      if (wrappedCursor instanceof AbstractCursor) {
        AbstractCursor ac = (AbstractCursor) wrappedCursor;
        return ac.getNotificationUri();
      }
    }

    return null;
  }
}
