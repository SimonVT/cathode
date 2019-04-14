/*
 * Copyright (C) 2017 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.sync.jobqueue

import android.text.format.DateUtils
import androidx.work.WorkManager
import net.simonvt.cathode.common.event.SyncEvent
import net.simonvt.cathode.common.util.MainHandler
import net.simonvt.cathode.jobqueue.Job
import net.simonvt.cathode.jobqueue.JobExecutor
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.work.enqueueNow
import net.simonvt.cathode.work.jobs.JobHandlerWorker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobHandler @Inject constructor(
  private val workManager: WorkManager,
  jobManager: JobManager
) {

  interface JobHandlerListener {

    fun onQueueEmpty()

    fun onQueueFailed()
  }

  private var executor: JobExecutor

  private val listeners = mutableListOf<JobHandlerListener>()

  private var executing = false

  private val stopRunnable = Runnable { stop() }

  private val executorListener = object : JobExecutor.JobExecutorListener {

    override fun onStartJob(job: Job) {
      executorStarted()
    }

    override fun onQueueEmpty() {
      executorStopped()
      dispatchQueueEmpty()
    }

    override fun onQueueFailed() {
      executorStopped()
      dispatchQueueFailed()
    }
  }

  init {
    executor = JobExecutor(jobManager, executorListener)
  }

  fun hasJobs() = executor.hasJobs()

  private fun dispatchQueueEmpty() {
    synchronized(listeners) {
      for (i in listeners.indices.reversed()) {
        val listener = listeners[i]
        listener.onQueueEmpty()
      }
    }
  }

  private fun dispatchQueueFailed() {
    synchronized(listeners) {
      for (i in listeners.indices.reversed()) {
        val listener = listeners[i]
        listener.onQueueFailed()
      }
    }
  }

  private fun start() {
    if (!executor.started) {
      executor.start()
    }
  }

  private fun stop() {
    Timber.d("[stop]")
    if (executor.started) {
      executor.stop()

      if (hasJobs()) {
        workManager.enqueueNow(JobHandlerWorker::class.java)
      }
    }
  }

  private fun executorStarted() {
    if (!executing) {
      executing = true
      SyncEvent.executorStarted()
    }
  }

  private fun executorStopped() {
    if (executing) {
      executing = false
      SyncEvent.executorStopped()
    }
  }

  fun registerListener(listener: JobHandlerListener) {
    synchronized(listeners) {
      listeners.add(listener)
      MainHandler.removeCallbacks(stopRunnable)
      start()
      Timber.d("%d listeners, resuming", listeners.size)
    }
  }

  fun unregisterListener(listener: JobHandlerListener) {
    synchronized(listeners) {
      listeners.remove(listener)

      if (listeners.isEmpty()) {
        Timber.d("No more listeners, posting stop")
        MainHandler.postDelayed(stopRunnable, STOP_DELAY)
      } else {
        Timber.d("%d listeners", listeners.size)
      }
    }
  }

  companion object {

    /**
     * Time to wait before stopping execution after all listeners are removed.
     */
    private const val STOP_DELAY = 2 * DateUtils.SECOND_IN_MILLIS
  }
}
