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
package net.simonvt.cathode.remote.sync.movies

import kotlinx.coroutines.runBlocking
import net.simonvt.cathode.actions.movies.SyncMovie
import net.simonvt.cathode.actions.movies.SyncMovie.Params
import net.simonvt.cathode.jobqueue.Job
import net.simonvt.cathode.jobqueue.JobPriority
import javax.inject.Inject

class SyncMovie(val traktId: Long) : Job() {

  @Inject
  @Transient
  lateinit var syncMovie: SyncMovie

  override fun key(): String {
    return "SyncMovie&traktId=$traktId"
  }

  override fun getPriority(): Int {
    return JobPriority.MOVIES
  }

  override fun perform(): Boolean {
    runBlocking { syncMovie(Params(traktId)) }
    return true
  }
}
