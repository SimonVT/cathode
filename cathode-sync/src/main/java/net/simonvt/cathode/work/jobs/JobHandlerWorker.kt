/*
 * Copyright (C) 2019 Simon Vig Therkildsen
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

package net.simonvt.cathode.work.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.suspendCancellableCoroutine
import net.simonvt.cathode.sync.jobqueue.JobHandler
import net.simonvt.cathode.sync.jobqueue.JobHandler.JobHandlerListener
import net.simonvt.cathode.work.ChildWorkerFactory

class JobHandlerWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted params: WorkerParameters,
  private val jobHandler: JobHandler
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    if (jobHandler.hasJobs()) {
      suspendCancellableCoroutine<Unit> { cont ->
        val listener = object : JobHandlerListener {
          override fun onQueueEmpty() {
            cont.cancel()
          }

          override fun onQueueFailed() {
            cont.cancel()
          }
        }

        cont.invokeOnCancellation { jobHandler.unregisterListener(listener) }
        jobHandler.registerListener(listener)
      }
    }

    return Result.success()
  }

  @AssistedInject.Factory
  interface Factory : ChildWorkerFactory

  companion object {
    const val TAG = "JobHandlerWorker"
    const val TAG_DAILY = "JobHandlerWorker_daily"
  }
}
