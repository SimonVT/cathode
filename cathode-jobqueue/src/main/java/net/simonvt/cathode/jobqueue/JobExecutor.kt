/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.jobqueue

import android.text.format.DateUtils
import net.simonvt.cathode.common.util.MainHandler
import timber.log.Timber
import java.util.concurrent.Executors

class JobExecutor(
  private val jobManager: JobManager,
  private val executorListener: JobExecutorListener
) {

  private val executor = Executors.newSingleThreadExecutor()

  var started = false
    private set

  private var failureBackoff: Boolean = false

  private val lock: Any = Any()

  private var executing = false

  private val jobListener = object : JobListener {

    override fun onJobAdded(job: Job) {
      startJobs()
    }
  }

  interface JobExecutorListener {

    fun onStartJob(job: Job)

    fun onQueueEmpty()

    fun onQueueFailed()
  }

  init {
    jobManager.addJobListener(jobListener)
  }

  fun start() {
    synchronized(lock) {
      if (!started) {
        started = true
        startJobs()
      }
    }
  }

  fun stop() {
    synchronized(lock) {
      started = false
    }
  }

  fun hasJobs(): Boolean {
    synchronized(lock) {
      return jobManager.hasJobs()
    }
  }

  private fun postOnStartJob(job: Job) {
    MainHandler.post { executorListener.onStartJob(job) }
  }

  private fun postQueueEmpty() {
    MainHandler.post {
      // Jobs might have been posted since postQueueEmpty was called,
      // can happen if last job in the queue posts additional jobs.
      if (jobManager.hasJobs()) {
        startJobs()
      } else executorListener.onQueueEmpty()
    }
  }

  private fun postQueueFailed() {
    MainHandler.post {
      executorListener.onQueueFailed()
    }
  }

  private fun startJobs() {
    synchronized(lock) {
      if (started && !failureBackoff && !executing) {
        val job = jobManager.nextJob()
        if (job != null) {
          executing = true
          Timber.d("Queueing job: %s", job.key())
          executor.execute(JobRunnable(job))
          postOnStartJob(job)
        }
      }
    }
  }

  private fun jobSucceeded(job: Job) {
    synchronized(lock) {
      Timber.d("Job succeded: %s", job.key())

      jobManager.removeJob(job)
      executing = false

      if (jobManager.hasJobs()) {
        startJobs()
      } else {
        postQueueEmpty()
      }
    }
  }

  private fun jobFailed(job: Job, t: Throwable) {
    synchronized(lock) {
      Timber.e(t, "Job failed: %s", job.key())
      jobFailed()
    }
  }

  private fun jobFailed() {
    synchronized(lock) {
      failureBackoff = true
      executing = false

      MainHandler.postDelayed({
        failureBackoff = false
        startJobs()
      }, FAILURE_DELAY)

      postQueueFailed()
    }
  }

  private inner class JobRunnable(val job: Job) : Runnable {

    override fun run() {
      try {
        Timber.d("Executing job: %s", job.key())
        if (job.perform()) {
          jobSucceeded(job)
        } else {
          jobFailed()
        }
      } catch (t: Throwable) {
        jobFailed(job, t)
      }
    }
  }

  companion object {

    /**
     * Execution is restarted after this delay on failure.
     */
    private const val FAILURE_DELAY = 30 * DateUtils.SECOND_IN_MILLIS
  }
}
