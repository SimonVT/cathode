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

package net.simonvt.cathode.util;

import android.database.Cursor;

public final class Cursors {

  private Cursors() {
  }

  public static String getString(Cursor cursor, String column) {
    return cursor.getString(cursor.getColumnIndex(column));
  }

  public static int getInt(Cursor cursor, String column) {
    return cursor.getInt(cursor.getColumnIndex(column));
  }

  public static long getLong(Cursor cursor, String column) {
    return cursor.getLong(cursor.getColumnIndex(column));
  }

  public static boolean getBoolean(Cursor cursor, String column) {
    return cursor.getInt(cursor.getColumnIndex(column)) == 1;
  }
}
