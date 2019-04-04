/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.sync.scheduler

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newFixedThreadPoolContext
import net.simonvt.cathode.jobqueue.Job
import net.simonvt.cathode.jobqueue.JobManager

open class BaseTaskScheduler(protected var context: Context, private val jobManager: JobManager) {

  private val dispatcher = newFixedThreadPoolContext(1, "TaskSchedulers")
  private val job = SupervisorJob()
  protected val scope = CoroutineScope(dispatcher + job)

  protected fun queue(task: Job) {
    jobManager.addJob(task)
  }
}
