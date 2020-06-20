package net.simonvt.cathode.work.shows

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.shows.MarkSyncUserShows
import net.simonvt.cathode.work.ChildWorkerFactory

class MarkSyncUserShowsWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted params: WorkerParameters,
  private val markSyncUserShows: MarkSyncUserShows
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    markSyncUserShows.invokeSync(Unit)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "MarkSyncUserShowsWorker"
    const val TAG_DAILY = "MarkSyncUserShowsWorker_daily"
  }
}
