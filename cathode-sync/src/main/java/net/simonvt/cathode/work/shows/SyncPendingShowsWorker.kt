package net.simonvt.cathode.work.shows

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.shows.SyncPendingShows
import net.simonvt.cathode.work.ChildWorkerFactory

class SyncPendingShowsWorker @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val syncPendingShows: SyncPendingShows
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    syncPendingShows.invokeSync(Unit)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "SyncPendingShowsWorker"
    const val TAG_DAILY = "SyncPendingShowsWorker_daily"
  }
}
