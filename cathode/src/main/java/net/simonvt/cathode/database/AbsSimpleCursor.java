/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2015 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.database;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

public abstract class AbsSimpleCursor implements Cursor {

  protected int pos;

  @Override public int getColumnCount() {
    return getColumnNames().length;
  }

  @Override public void deactivate() {
    // NO-OP
  }

  @Override public boolean requery() {
    throw new RuntimeException("Not supported");
  }

  @Override public boolean isClosed() {
    return false;
  }

  @Override public void close() {
    // NO-OP
  }

  /**
   * This function is called every time the cursor is successfully scrolled
   * to a new position, giving the subclass a chance to update any state it
   * may have. If it returns false the move function will also do so and the
   * cursor will scroll to the beforeFirst position.
   *
   * @param oldPosition the position that we're moving from
   * @param newPosition the position that we're moving to
   * @return true if the move is successful, false otherwise
   */
  public boolean onMove(int oldPosition, int newPosition) {
    return true;
  }

  public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
    // Default implementation, uses getString
    String result = getString(columnIndex);
    if (result != null) {
      char[] data = buffer.data;
      if (data == null || data.length < result.length()) {
        buffer.data = result.toCharArray();
      } else {
        result.getChars(0, result.length(), data, 0);
      }
      buffer.sizeCopied = result.length();
    } else {
      buffer.sizeCopied = 0;
    }
  }

  /* -------------------------------------------------------- */
    /* Implementation */
  public AbsSimpleCursor() {
    pos = -1;
  }

  @Override public final int getPosition() {
    return pos;
  }

  @Override public final boolean moveToPosition(int position) {
    // Make sure position isn't past the end of the cursor
    final int count = getCount();
    if (position >= count) {
      pos = count;
      return false;
    }

    // Make sure position isn't before the beginning of the cursor
    if (position < 0) {
      pos = -1;
      return false;
    }

    // Check for no-op moves, and skip the rest of the work for them
    if (position == pos) {
      return true;
    }

    boolean result = onMove(pos, position);
    if (!result) {
      pos = -1;
    } else {
      pos = position;
    }

    return result;
  }

  @Override public final boolean move(int offset) {
    return moveToPosition(pos + offset);
  }

  @Override public final boolean moveToFirst() {
    return moveToPosition(0);
  }

  @Override public final boolean moveToLast() {
    return moveToPosition(getCount() - 1);
  }

  @Override public final boolean moveToNext() {
    return moveToPosition(pos + 1);
  }

  @Override public final boolean moveToPrevious() {
    return moveToPosition(pos - 1);
  }

  @Override public final boolean isFirst() {
    return pos == 0 && getCount() != 0;
  }

  @Override public final boolean isLast() {
    int cnt = getCount();
    return pos == (cnt - 1) && cnt != 0;
  }

  @Override public final boolean isBeforeFirst() {
    if (getCount() == 0) {
      return true;
    }
    return pos == -1;
  }

  @Override public final boolean isAfterLast() {
    if (getCount() == 0) {
      return true;
    }
    return pos == getCount();
  }

  @Override public int getColumnIndex(String columnName) {
    // Hack according to bug 903852
    final int periodIndex = columnName.lastIndexOf('.');
    if (periodIndex != -1) {
      columnName = columnName.substring(periodIndex + 1);
    }

    String[] columnNames = getColumnNames();
    int length = columnNames.length;
    for (int i = 0; i < length; i++) {
      if (columnNames[i].equalsIgnoreCase(columnName)) {
        return i;
      }
    }

    return -1;
  }

  @Override public int getColumnIndexOrThrow(String columnName) {
    final int index = getColumnIndex(columnName);
    if (index < 0) {
      throw new IllegalArgumentException("column '" + columnName + "' does not exist");
    }
    return index;
  }

  @Override public String getColumnName(int columnIndex) {
    return getColumnNames()[columnIndex];
  }

  @Override public void registerContentObserver(ContentObserver observer) {
    throw new RuntimeException("Not supported");
  }

  @Override public void unregisterContentObserver(ContentObserver observer) {
    throw new RuntimeException("Not supported");
  }

  @Override public void registerDataSetObserver(DataSetObserver observer) {
    throw new RuntimeException("Not supported");
  }

  @Override public void unregisterDataSetObserver(DataSetObserver observer) {
    throw new RuntimeException("Not supported");
  }

  @Override public void setNotificationUri(ContentResolver cr, Uri notifyUri) {
    throw new RuntimeException("Not supported");
  }

  @Override public Uri getNotificationUri() {
    throw new RuntimeException("Not supported");
  }

  @Override public boolean getWantsAllOnMoveCalls() {
    return false;
  }

  @Override public Bundle respond(Bundle extras) {
    return Bundle.EMPTY;
  }

  @Override public Bundle getExtras() {
    return Bundle.EMPTY;
  }

  @Override public void setExtras(Bundle extras) {
    throw new RuntimeException("Not supported");
  }

  @Override protected void finalize() {
  }
}
