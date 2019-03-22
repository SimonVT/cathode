package net.simonvt.cathode.provider

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.OperationApplicationException
import android.database.Cursor
import android.net.Uri
import android.os.RemoteException
import net.simonvt.cathode.provider.generated.CathodeProvider
import timber.log.Timber

fun ContentResolver.batch(ops: ArrayList<ContentProviderOperation>) {
  try {
    applyBatch(CathodeProvider.AUTHORITY, ops)
  } catch (e: RemoteException) {
    Timber.e(e)
    throw RuntimeException(e)
  } catch (e: OperationApplicationException) {
    Timber.e(e)
    throw RuntimeException(e)
  }
}

fun ContentResolver.query(
  uri: Uri,
  projection: Array<String>? = null,
  where: String? = null,
  whereArgs: Array<String>? = null
): Cursor = query(uri, projection, where, whereArgs, null)!!

fun ContentResolver.update(uri: Uri, values: ContentValues, where: String? = null): Int =
  update(uri, values, where, null)

fun ContentResolver.delete(uri: Uri, where: String? = null): Int = delete(uri, where, null)
