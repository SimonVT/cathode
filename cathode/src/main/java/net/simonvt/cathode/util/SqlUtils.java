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
import android.database.sqlite.SQLiteDatabase;
import java.util.HashSet;
import java.util.Set;
import net.simonvt.schematic.annotation.DataType;

public final class SqlUtils {

  private SqlUtils() {
  }

  public static Set<String> columns(SQLiteDatabase db, String table) {
    Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
    Set<String> columns = new HashSet<>();

    while (cursor.moveToNext()) {
      final String name = cursor.getString(cursor.getColumnIndex("name"));
      columns.add(name);
    }

    cursor.close();

    return columns;
  }

  public static boolean createColumnIfNotExists(SQLiteDatabase db, String table, String columnName,
      DataType.Type type, String defaultValue) {
    Set<String> columns = columns(db, table);

    if (!columns.contains(columnName)) {
      db.execSQL(
          "ALTER TABLE " + table + " ADD COLUMN " + columnName + " " + type.toString() + " DEFAULT "
              + defaultValue);
      return true;
    }

    return false;
  }
}
