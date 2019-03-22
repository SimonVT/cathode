package net.simonvt.cathode.work.shows

import android.content.ContentValues
import android.content.Context
import android.text.format.DateUtils
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.work.ChildWorkerFactory

class MarkSyncUserShowsWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    val syncBefore = System.currentTimeMillis() - SYNC_INTERVAL
    val values = ContentValues()
    values.put(ShowColumns.NEEDS_SYNC, true)
    context.contentResolver.update(
      Shows.SHOWS, values, "(" +
          ShowColumns.WATCHED_COUNT + ">0 OR " +
          ShowColumns.IN_COLLECTION_COUNT + ">0 OR " +
          ShowColumns.IN_WATCHLIST_COUNT + ">0 OR " +
          ShowColumns.IN_WATCHLIST + ") AND " +
          ShowColumns.LAST_SYNC + "<?",
      arrayOf(syncBefore.toString())
    )
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "MarkSyncUserShowsWorker"
    const val TAG_DAILY = "MarkSyncUserShowsWorker_daily"
    const val SYNC_INTERVAL = 30 * DateUtils.DAY_IN_MILLIS
  }
}
