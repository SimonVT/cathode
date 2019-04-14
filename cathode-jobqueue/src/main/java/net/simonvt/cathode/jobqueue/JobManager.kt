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

import android.content.ContentValues
import android.content.Context
import dagger.android.DispatchingAndroidInjector
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.common.util.MainHandler
import net.simonvt.cathode.jobqueue.JobDatabaseSchematic.Tables
import net.simonvt.cathode.jobqueue.database.JobDatabase
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobManager @Inject constructor(
  private val context: Context,
  private val jobInjector: DispatchingAndroidInjector<Job>
) {

  private val database = JobDatabase.getInstance(this.context)

  private val converter = Converter()

  private val jobs = mutableListOf<Job>()

  private val serialExecutor = Executors.newSingleThreadExecutor()

  private val jobListeners = ArrayList<WeakReference<JobListener>>()

  init {
    serialExecutor.execute { loadJobs() }
  }

  fun addJobListener(listener: JobListener) {
    val jobListener = WeakReference(listener)
    jobListeners.add(jobListener)
  }

  fun removeJobListener(listener: JobListener) {
    for (i in jobListeners.indices.reversed()) {
      val listenerRef = jobListeners[i]
      val jobListener = listenerRef.get()
      if (jobListener == null || listener === jobListener) {
        jobListeners.remove(listenerRef)
      }
    }
  }

  private fun postOnJobAdded(job: Job) {
    MainHandler.post {
      for (i in jobListeners.indices.reversed()) {
        val listenerRef = jobListeners[i]
        val jobListener = listenerRef.get()

        jobListener?.onJobAdded(job) ?: jobListeners.remove(listenerRef)
      }
    }
  }

  private fun loadJobs() {
    synchronized(jobs) {
      val db = database.readableDatabase
      val c = db.query(Tables.JOBS, null, null, null, null, null, null)

      while (c.moveToNext()) {
        val bytes = Cursors.getBlob(c, JobColumns.JOB)
        val job = converter.from(bytes)
        addJobInternal(job)
      }

      c.close()

      Timber.d("Loaded %d jobs", jobs.size)
    }
  }

  fun addJob(job: Job) {
    serialExecutor.execute { addJobNow(job) }
  }

  fun addJobNow(job: Job) {
    Timber.d("Adding job: %s", job.key())
    postOnJobAdded(job)

    synchronized(jobs) {
      addJobInternal(job)
    }

    persistJob(job)
  }

  private fun addJobInternal(job: Job) {
    synchronized(jobs) {
      jobs.add(job)
    }
  }

  private fun persistJob(job: Job) {
    val values = ContentValues()
    values.put(JobColumns.KEY, job.key())
    val bytes = converter.to(job)
    values.put(JobColumns.JOB, bytes)
    values.put(JobColumns.JOB_NAME, job.javaClass.name)

    val db = database.writableDatabase
    db.insert(Tables.JOBS, null, values)
  }

  fun nextJob(): Job? {
    synchronized(jobs) {
      if (jobs.size > 0) {
        val job = jobs[0]
        jobInjector.inject(job)
        return job
      }

      return null
    }
  }

  fun removeJob(job: Job) {
    synchronized(jobs) {
      Timber.d("Removing job: %s", job.key())
      jobs.remove(job)
    }

    removeJobFromDatabase(job)
  }

  private fun removeJobFromDatabase(job: Job) {
    serialExecutor.execute {
      val db = database.writableDatabase
      db.delete(
        Tables.JOBS,
        JobColumns.JOB_NAME + "=? AND " + JobColumns.KEY + "=?",
        arrayOf(job.javaClass.name, job.key())
      )
    }
  }

  fun hasJobs(): Boolean {
    synchronized(jobs) {
      return jobs.size > 0
    }
  }

  fun clear() {
    serialExecutor.execute {
      synchronized(jobs) {
        jobs.clear()

        val db = database.writableDatabase
        db.delete(Tables.JOBS, null, null)
      }
    }
  }
}
