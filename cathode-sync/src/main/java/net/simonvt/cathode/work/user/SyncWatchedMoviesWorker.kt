package net.simonvt.cathode.work.user

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.user.SyncWatchedMovies
import net.simonvt.cathode.work.ChildWorkerFactory

class SyncWatchedMoviesWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted val params: WorkerParameters,
  private val syncWatchedMovies: SyncWatchedMovies
) : CoroutineWorker(context, params) {

  override val coroutineContext = Dispatchers.IO

  override suspend fun doWork(): Result = coroutineScope {
    syncWatchedMovies.invokeSync(SyncWatchedMovies.Params())
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory
}
