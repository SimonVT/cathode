package net.simonvt.cathode.common.database;

import android.database.Cursor;

public final class Cursors {


  private Cursors() {
  }

  public static String getString(Cursor cursor, String column) {
    return cursor.getString(cursor.getColumnIndexOrThrow(column));
  }

  public static String getStringOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndex(column);
    if (index == -1 || cursor.isNull(index)) {
      return null;
    }

    return cursor.getString(index);
  }

  public static int getInt(Cursor cursor, String column) {
    return cursor.getInt(cursor.getColumnIndexOrThrow(column));
  }

  public static Integer getIntOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndex(column);
    if (index == -1 || cursor.isNull(index)) {
      return null;
    }

    return cursor.getInt(index);
  }

  public static long getLong(Cursor cursor, String column) {
    return cursor.getLong(cursor.getColumnIndexOrThrow(column));
  }

  public static Long getLongOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndex(column);
    if (index == -1 || cursor.isNull(index)) {
      return null;
    }

    return cursor.getLong(index);
  }

  public static float getFloat(Cursor cursor, String column) {
    return cursor.getFloat(cursor.getColumnIndexOrThrow(column));
  }

  public static Float getFloatOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndex(column);
    if (index == -1 || cursor.isNull(index)) {
      return null;
    }

    return cursor.getFloat(index);
  }

  public static double getDouble(Cursor cursor, String column) {
    return cursor.getDouble(cursor.getColumnIndexOrThrow(column));
  }

  public static Double getDoubleOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndex(column);
    if (index == -1 || cursor.isNull(index)) {
      return null;
    }

    return cursor.getDouble(index);
  }

  public static boolean getBoolean(Cursor cursor, String column) {
    return cursor.getInt(cursor.getColumnIndexOrThrow(column)) == 1;
  }

  public static Boolean getBooleanOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndex(column);
    if (index == -1 || cursor.isNull(index)) {
      return null;
    }

    return cursor.getInt(index) == 1;
  }

  public static byte[] getBlob(Cursor cursor, String column) {
    return cursor.getBlob(cursor.getColumnIndexOrThrow(column));
  }

  public static byte[] getBlobOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndex(column);
    if (index == -1 || cursor.isNull(index)) {
      return null;
    }

    return cursor.getBlob(index);
  }
}
