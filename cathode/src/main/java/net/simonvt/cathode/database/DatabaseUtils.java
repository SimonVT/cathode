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
