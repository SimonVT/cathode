package net.simonvt.cathode.work.movies

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.movies.SyncPendingMovies
import net.simonvt.cathode.work.ChildWorkerFactory

class SyncPendingMoviesWorker @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val syncPendingMovies: SyncPendingMovies
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    syncPendingMovies.invokeSync(Unit)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "SyncPendingMoviesWorker"
    const val TAG_DAILY = "SyncPendingMoviesWorker_daily"
  }
}
