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
package net.simonvt.cathode.common.database;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

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

  public static String removeLeadingArticle(String string) {
    if (TextUtils.isEmpty(string)) {
      return string;
    }

    final long length = string.length();

    if (length > 4 && (string.startsWith("The ") || string.startsWith("the "))) {
      return string.substring(4);
    }

    if (length > 3 && (string.startsWith("An ") || string.startsWith("an "))) {
      return string.substring(3);
    }

    if (length > 2 && (string.startsWith("A ") || string.startsWith("a "))) {
      return string.substring(2);
    }

    return string;
  }
}
