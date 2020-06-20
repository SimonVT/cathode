package net.simonvt.cathode.work.user

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncUserSettings
import net.simonvt.cathode.work.ChildWorkerFactory

class SyncUserSettingsWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted val params: WorkerParameters,
  private val syncUserSettings: SyncUserSettings
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    syncUserSettings.invokeSync(Unit)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory
}
