package net.simonvt.cathode.work

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingWorkPolicy.APPEND
import androidx.work.ListenableWorker
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

fun WorkManager.enqueueNow(
  clazz: Class<out ListenableWorker>,
  data: Data? = null,
  requiresNetwork: Boolean = true
) {
  enqueueDelayed(clazz = clazz, data = data, requiresNetwork = requiresNetwork)
}

fun WorkManager.enqueueUniqueNow(
  tag: String,
  clazz: Class<out ListenableWorker>,
  existingWorkPolicy: ExistingWorkPolicy = APPEND,
  data: Data? = null,
  requiresNetwork: Boolean = true
) {
  Timber.d("Enqueueing %s", clazz.name)
  val requestBuilder = OneTimeWorkRequest.Builder(clazz)
  val constraintsBuilder = Constraints.Builder()
  if (data != null) {
    requestBuilder.setInputData(data)
  }
  if (requiresNetwork) {
    constraintsBuilder.setRequiredNetworkType(CONNECTED)
  }
  requestBuilder.setConstraints(constraintsBuilder.build())
  enqueueUniqueWork(tag, existingWorkPolicy, requestBuilder.build())
}

fun WorkManager.enqueueDelayed(
  clazz: Class<out ListenableWorker>,
  delay: Long = 0L,
  data: Data? = null,
  requiresNetwork: Boolean = true
) {
  Timber.d("Enqueueing %s", clazz.name)
  val requestBuilder = OneTimeWorkRequest.Builder(clazz)
  if (delay > 0L) {
    requestBuilder.setInitialDelay(delay, MILLISECONDS)
  }
  if (data != null) {
    requestBuilder.setInputData(data)
  }
  if (requiresNetwork) {
    val constraintsBuilder = Constraints.Builder().setRequiredNetworkType(CONNECTED)
    requestBuilder.setConstraints(constraintsBuilder.build())
  }
  enqueue(requestBuilder.build())
  Timber.d("Enqueued %s", clazz.name)
}

fun WorkManager.enqueueDaily(
  clazz: Class<out ListenableWorker>,
  tag: String,
  constraints: Constraints,
  existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy = KEEP
) {
  val requestBuilder = PeriodicWorkRequest.Builder(clazz, 1L, TimeUnit.DAYS)
  requestBuilder.setConstraints(constraints)
  enqueueUniquePeriodicWork(tag, existingPeriodicWorkPolicy, requestBuilder.build())
}
