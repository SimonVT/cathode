package net.simonvt.cathode.work.movies

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.movies.SyncUpdatedMovies
import net.simonvt.cathode.work.ChildWorkerFactory
import net.simonvt.cathode.work.enqueueNow

class SyncUpdatedMoviesWorker @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val workManager: WorkManager,
  private val syncUpdatedMovies: SyncUpdatedMovies
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    syncUpdatedMovies.invokeSync(Unit)
    workManager.enqueueNow(SyncPendingMoviesWorker::class.java)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "SyncUpdatedMoviesWorker"
    const val TAG_DAILY = "SyncUpdatedMoviesWorker_daily"
  }
}
