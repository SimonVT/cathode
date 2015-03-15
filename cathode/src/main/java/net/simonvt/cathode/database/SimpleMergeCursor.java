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

public class SimpleMergeCursor extends AbsSimpleCursor {

  public SimpleMergeCursor(Cursor... cursors) {
    Cursor[] simpleCursors = new SimpleCursor[cursors.length];

    for (int i = 0; i < cursors.length; i++) {
      Cursor cursor = cursors[i];
      Cursor simpleCursor = new SimpleCursor(cursor);
      simpleCursors[i] = simpleCursor;
      cursor.close();
    }

    mCursors = simpleCursors;
    mCursor = cursors[0];
  }

  @Override public int getCount() {
    int count = 0;
    int length = mCursors.length;
    for (int i = 0; i < length; i++) {
      if (mCursors[i] != null) {
        count += mCursors[i].getCount();
      }
    }
    return count;
  }

  @Override public boolean onMove(int oldPosition, int newPosition) {
        /* Find the right cursor */
    mCursor = null;
    int cursorStartPos = 0;
    int length = mCursors.length;
    for (int i = 0; i < length; i++) {
      if (mCursors[i] == null) {
        continue;
      }

      if (newPosition < (cursorStartPos + mCursors[i].getCount())) {
        mCursor = mCursors[i];
        break;
      }

      cursorStartPos += mCursors[i].getCount();
    }

        /* Move it to the right position */
    if (mCursor != null) {
      boolean ret = mCursor.moveToPosition(newPosition - cursorStartPos);
      return ret;
    }

    return false;
  }

  @Override public String getString(int column) {
    return mCursor.getString(column);
  }

  @Override public short getShort(int column) {
    return mCursor.getShort(column);
  }

  @Override public int getInt(int column) {
    return mCursor.getInt(column);
  }

  @Override public long getLong(int column) {
    return mCursor.getLong(column);
  }

  @Override public float getFloat(int column) {
    return mCursor.getFloat(column);
  }

  @Override public double getDouble(int column) {
    return mCursor.getDouble(column);
  }

  @Override public int getType(int column) {
    return mCursor.getType(column);
  }

  @Override public boolean isNull(int column) {
    return mCursor.isNull(column);
  }

  @Override public byte[] getBlob(int column) {
    return mCursor.getBlob(column);
  }

  @Override public String[] getColumnNames() {
    if (mCursor != null) {
      return mCursor.getColumnNames();
    } else {
      return new String[0];
    }
  }

  private Cursor mCursor; // updated in onMove
  private Cursor[] mCursors;
}
