package net.simonvt.cathode.work.movies

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.simonvt.cathode.actions.invokeSync
import net.simonvt.cathode.actions.movies.MarkSyncUserMovies
import net.simonvt.cathode.work.ChildWorkerFactory

class MarkSyncUserMoviesWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted params: WorkerParameters,
  private val markSyncUserMovies: MarkSyncUserMovies
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    markSyncUserMovies.invokeSync(Unit)
    Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "MarkSyncUserMoviesWorker"
    const val TAG_DAILY = "MarkSyncUserMoviesWorker"
  }
}
