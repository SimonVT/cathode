package net.simonvt.cathode.work.user

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.user.SyncWatching
import net.simonvt.cathode.work.ChildWorkerFactory

class SyncWatchingWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted params: WorkerParameters,
  private val syncWatching: SyncWatching
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    syncWatching(Unit)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory
}
