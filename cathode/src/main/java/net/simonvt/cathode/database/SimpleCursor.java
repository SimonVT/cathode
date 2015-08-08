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

import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.provider.BaseColumns;
import java.util.ArrayList;
import java.util.List;

/**
 * A mutable cursor implementation backed by an {@link java.util.ArrayList} of {@code Object}s.
 */
public class SimpleCursor extends AbsSimpleCursor {

  private final String[] columnNames;
  private List<Object[]> data = new ArrayList<Object[]>();
  private final int columnCount;

  /**
   * Constructs a new cursor with the given initial capacity.
   *
   * @param columnNames names of the columns, the ordering of which
   * determines column ordering elsewhere in this cursor
   */
  public SimpleCursor(String[] columnNames) {
    this.columnNames = columnNames;
    this.columnCount = columnNames.length;
  }

  /**
   * Constructs a SimpleCursor from the data in source.
   */
  public SimpleCursor(Cursor source) {
    this.columnNames = source.getColumnNames();
    this.columnCount = this.columnNames.length;

    final int columnCount = source.getColumnCount();
    while (source.moveToNext()) {
      Object[] data = new Object[columnCount];
      for (int i = 0; i < columnCount; i++) {
        final int type = source.getType(i);
        switch (type) {
          case Cursor.FIELD_TYPE_BLOB:
            data[i] = source.getBlob(i);
            break;

          case Cursor.FIELD_TYPE_FLOAT:
            data[i] = source.getFloat(i);
            break;

          case Cursor.FIELD_TYPE_INTEGER:
            data[i] = source.getLong(i);
            break;

          case Cursor.FIELD_TYPE_STRING:
            data[i] = source.getString(i);
            break;
        }
      }
      add(data);
    }
  }

  private Object get(int column) {
    if (column < 0 || column >= columnCount) {
      throw new CursorIndexOutOfBoundsException(
          "Requested column: " + column + ", # of columns: " + columnCount);
    }
    if (mPos < 0) {
      throw new CursorIndexOutOfBoundsException("Before first row.");
    }
    if (mPos >= data.size()) {
      throw new CursorIndexOutOfBoundsException("After last row.");
    }
    return data.get(mPos)[column];
  }

  /**
   * Adds a new row to the end with the given column values. Not safe
   * for concurrent use.
   *
   * @param columnValues in the same order as the the column names specified
   * at cursor construction time
   * @throws IllegalArgumentException if {@code columnValues.length !=
   * columnNames.length}
   */
  public void add(Object[] columnValues) {
    if (columnValues.length != columnCount) {
      throw new IllegalArgumentException(
          "columnNames.length = " + columnCount + ", columnValues.length = " + columnValues.length);
    }

    data.add(columnValues);
  }

  public Object[] get() {
    return data.get(mPos);
  }

  /**
   * Adds a new row to the end with the given column values. Not safe
   * for concurrent use.
   *
   * @param columnValues in the same order as the the column names specified at cursor construction
   * time
   * @param position The position at which to insert columnValues
   * @throws IllegalArgumentException if {@code columnValues.length != columnNames.length}
   */
  public void add(Object[] columnValues, int position) {
    if (columnValues.length != columnCount) {
      throw new IllegalArgumentException(
          "columnNames.length = " + columnCount + ", columnValues.length = " + columnValues.length);
    }

    data.add(position, columnValues);
  }

  /**
   * Removes the item at the specified position.
   */
  public void remove(int position) {
    data.remove(position);
  }

  public void remove(long id) {
    if (hasColumn(BaseColumns._ID)) {
      final int idColumn = getColumnIndex(BaseColumns._ID);
      for (int position = 0; position < getCount(); position++) {
        Object[] values = data.get(position);
        final long rowId = Long.valueOf(values[idColumn].toString());
        if (id == rowId) {
          data.remove(position);
          break;
        }
      }
    }
  }

  @Override public int getCount() {
    return data.size();
  }

  @Override public String[] getColumnNames() {
    return columnNames;
  }

  public boolean hasColumn(String column) {
    for (String col : columnNames) {
      if (column.equals(col)) {
        return true;
      }
    }

    return false;
  }

  @Override public String getString(int column) {
    Object value = get(column);
    if (value == null) return null;
    return value.toString();
  }

  @Override public short getShort(int column) {
    Object value = get(column);
    if (value == null) return 0;
    if (value instanceof Number) return ((Number) value).shortValue();
    return Short.parseShort(value.toString());
  }

  @Override public int getInt(int column) {
    Object value = get(column);
    if (value == null) return 0;
    if (value instanceof Number) return ((Number) value).intValue();
    return Integer.parseInt(value.toString());
  }

  @Override public long getLong(int column) {
    Object value = get(column);
    if (value == null) return 0;
    if (value instanceof Number) return ((Number) value).longValue();
    return Long.parseLong(value.toString());
  }

  @Override public float getFloat(int column) {
    Object value = get(column);
    if (value == null) return 0.0f;
    if (value instanceof Number) return ((Number) value).floatValue();
    return Float.parseFloat(value.toString());
  }

  @Override public double getDouble(int column) {
    Object value = get(column);
    if (value == null) return 0.0d;
    if (value instanceof Number) return ((Number) value).doubleValue();
    return Double.parseDouble(value.toString());
  }

  @Override public byte[] getBlob(int column) {
    Object value = get(column);
    return (byte[]) value;
  }

  @Override public int getType(int column) {
    return getTypeOfObject(get(column));
  }

  public static int getTypeOfObject(Object obj) {
    if (obj == null) {
      return FIELD_TYPE_NULL;
    } else if (obj instanceof byte[]) {
      return FIELD_TYPE_BLOB;
    } else if (obj instanceof Float || obj instanceof Double) {
      return FIELD_TYPE_FLOAT;
    } else if (obj instanceof Long
        || obj instanceof Integer
        || obj instanceof Short
        || obj instanceof Byte) {
      return FIELD_TYPE_INTEGER;
    } else {
      return FIELD_TYPE_STRING;
    }
  }

  @Override public boolean isNull(int column) {
    return get(column) == null;
  }
}
