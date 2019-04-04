package net.simonvt.cathode.work.shows

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.shows.SyncUpdatedShows
import net.simonvt.cathode.work.ChildWorkerFactory
import net.simonvt.cathode.work.enqueueNow

class SyncUpdatedShowsWorker @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val workManager: WorkManager,
  private val syncUpdatedShows: SyncUpdatedShows
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    syncUpdatedShows.invokeSync(Unit)
    workManager.enqueueNow(SyncPendingShowsWorker::class.java)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "SyncUpdatedShowsWorker"
    const val TAG_DAILY = "SyncUpdatedShowsWorker_daily"
  }
}
