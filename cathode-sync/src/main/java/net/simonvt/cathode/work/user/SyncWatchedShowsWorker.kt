package net.simonvt.cathode.work.user

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncWatchedShows
import net.simonvt.cathode.work.ChildWorkerFactory

class SyncWatchedShowsWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted val params: WorkerParameters,
  private val syncWatchedShows: SyncWatchedShows
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    syncWatchedShows.invokeSync(SyncWatchedShows.Params())
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory
}
