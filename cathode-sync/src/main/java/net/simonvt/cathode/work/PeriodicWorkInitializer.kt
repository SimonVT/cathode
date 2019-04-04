package net.simonvt.cathode.work

import android.os.Build
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import net.simonvt.cathode.work.jobs.JobHandlerWorker
import net.simonvt.cathode.work.movies.MarkSyncUserMoviesWorker
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import net.simonvt.cathode.work.movies.SyncUpdatedMoviesWorker
import net.simonvt.cathode.work.shows.MarkSyncUserShowsWorker
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import net.simonvt.cathode.work.shows.SyncUpdatedShowsWorker
import net.simonvt.cathode.work.user.SyncUserActivityWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodicWorkInitializer @Inject constructor(private val workManager: WorkManager) {

  fun init() {
    val constraintsBuilder = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresCharging(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      constraintsBuilder.setRequiresDeviceIdle(true)
    }
    val constraints = constraintsBuilder.build()

    workManager.enqueueDaily(
      SyncUpdatedShowsWorker::class.java,
      SyncUpdatedShowsWorker.TAG_DAILY,
      constraints
    )
    workManager.enqueueDaily(
      SyncUpdatedMoviesWorker::class.java,
      SyncUpdatedMoviesWorker.TAG_DAILY,
      constraints
    )
    workManager.enqueueDaily(
      MarkSyncUserShowsWorker::class.java,
      MarkSyncUserShowsWorker.TAG_DAILY,
      constraints
    )
    workManager.enqueueDaily(
      MarkSyncUserMoviesWorker::class.java,
      MarkSyncUserMoviesWorker.TAG_DAILY,
      constraints
    )
    workManager.enqueueDaily(
      SyncPendingShowsWorker::class.java,
      SyncPendingShowsWorker.TAG_DAILY,
      constraints
    )
    workManager.enqueueDaily(
      SyncPendingMoviesWorker::class.java,
      SyncPendingMoviesWorker.TAG_DAILY,
      constraints
    )
  }

  fun initAuthWork() {
    val constraintsBuilder = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresCharging(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      constraintsBuilder.setRequiresDeviceIdle(true)
    }
    val constraints = constraintsBuilder.build()

    workManager.enqueueDaily(
      JobHandlerWorker::class.java,
      JobHandlerWorker.TAG_DAILY,
      constraints
    )
    workManager.enqueueDaily(
      SyncUserActivityWorker::class.java,
      SyncUserActivityWorker.TAG_DAILY,
      constraints
    )
  }

  fun cancelAuthWork() {
    workManager.cancelAllWorkByTag(JobHandlerWorker.TAG_DAILY)
    workManager.cancelAllWorkByTag(SyncUserActivityWorker.TAG_DAILY)
  }
}
