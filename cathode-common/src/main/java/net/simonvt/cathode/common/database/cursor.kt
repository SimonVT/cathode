package net.simonvt.cathode.common.database

import android.database.Cursor

fun Cursor.getBlob(column: String): ByteArray = getBlob(this.getColumnIndexOrThrow(column))
fun Cursor.getDouble(column: String): Double = getDouble(this.getColumnIndexOrThrow(column))
fun Cursor.getFloat(column: String): Float = getFloat(this.getColumnIndexOrThrow(column))
fun Cursor.getInt(column: String): Int = getInt(getColumnIndexOrThrow(column))
fun Cursor.getLong(column: String): Long = getLong(getColumnIndexOrThrow(column))
fun Cursor.getString(column: String): String = getString(this.getColumnIndexOrThrow(column))
fun Cursor.getStringOrNull(column: String): String? {
  val index = getColumnIndex(column)
  if (index == -1 || isNull(index)) {
    return null
  }

  return getString(column)
}
fun Cursor.getBoolean(column: String): Boolean = getInt(column) == 1

inline fun Cursor.forEach(action: (Cursor) -> Unit) {
  moveToPosition(-1)
  while (moveToNext()) {
    action(this)
  }
}
