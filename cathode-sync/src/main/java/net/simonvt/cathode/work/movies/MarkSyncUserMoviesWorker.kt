package net.simonvt.cathode.work.movies

import android.content.ContentValues
import android.content.Context
import android.text.format.DateUtils
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.ProviderSchematic.Movies
import net.simonvt.cathode.work.ChildWorkerFactory

class MarkSyncUserMoviesWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    val syncBefore = System.currentTimeMillis() - SYNC_INTERVAL
    val values = ContentValues()
    values.put(MovieColumns.NEEDS_SYNC, true)
    context.contentResolver.update(
      Movies.MOVIES, values, "(" +
          MovieColumns.WATCHED + " OR " +
          MovieColumns.IN_COLLECTION + " OR " +
          MovieColumns.IN_WATCHLIST + ") AND " +
          MovieColumns.LAST_SYNC + "<?",
      arrayOf(syncBefore.toString())
    )
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "MarkSyncUserMoviesWorker"
    const val TAG_DAILY = "MarkSyncUserMoviesWorker"
    const val SYNC_INTERVAL = 30 * DateUtils.DAY_IN_MILLIS
  }
}
