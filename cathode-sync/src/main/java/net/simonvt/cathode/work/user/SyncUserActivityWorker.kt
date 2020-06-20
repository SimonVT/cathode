package net.simonvt.cathode.work.user

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncUserActivity
import net.simonvt.cathode.work.ChildWorkerFactory

class SyncUserActivityWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted val params: WorkerParameters,
  private val syncUserActivity: SyncUserActivity
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    syncUserActivity.invokeSync(Unit)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG_DAILY = "SyncUserActivityWorker_daily"
  }
}
