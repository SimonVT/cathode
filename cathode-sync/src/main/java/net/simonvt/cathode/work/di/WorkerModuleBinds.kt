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

package net.simonvt.cathode.work.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import net.simonvt.cathode.work.ChildWorkerFactory
import net.simonvt.cathode.work.jobs.JobHandlerWorker
import net.simonvt.cathode.work.movies.MarkSyncUserMoviesWorker
import net.simonvt.cathode.work.movies.SyncPendingMoviesWorker
import net.simonvt.cathode.work.movies.SyncUpdatedMoviesWorker
import net.simonvt.cathode.work.shows.MarkSyncUserShowsWorker
import net.simonvt.cathode.work.shows.SyncPendingShowsWorker
import net.simonvt.cathode.work.shows.SyncUpdatedShowsWorker
import net.simonvt.cathode.work.user.PeriodicSyncWorker
import net.simonvt.cathode.work.user.SyncUserActivityWorker
import net.simonvt.cathode.work.user.SyncUserSettingsWorker
import net.simonvt.cathode.work.user.SyncWatchedMoviesWorker
import net.simonvt.cathode.work.user.SyncWatchedShowsWorker
import net.simonvt.cathode.work.user.SyncWatchingWorker

@Module
abstract class WorkerModuleBinds {

  /* Jobs */
  @Binds
  @IntoMap
  @WorkerKey(JobHandlerWorker::class)
  abstract fun jobHandlerWorker(factory: JobHandlerWorker.Factory): ChildWorkerFactory

  /* Movies */
  @Binds
  @IntoMap
  @WorkerKey(MarkSyncUserMoviesWorker::class)
  abstract fun markSyncUserMoviesWorker(factory: MarkSyncUserMoviesWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncPendingMoviesWorker::class)
  abstract fun syncMoviesWorker(factory: SyncPendingMoviesWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncUpdatedMoviesWorker::class)
  abstract fun syncUpdatedMoviesWorker(factory: SyncUpdatedMoviesWorker.Factory): ChildWorkerFactory

  /* Shows */
  @Binds
  @IntoMap
  @WorkerKey(MarkSyncUserShowsWorker::class)
  abstract fun markSyncUserShowsWorker(factory: MarkSyncUserShowsWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncPendingShowsWorker::class)
  abstract fun syncShowsWorker(factory: SyncPendingShowsWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncUpdatedShowsWorker::class)
  abstract fun syncUpdatedShowsWorker(factory: SyncUpdatedShowsWorker.Factory): ChildWorkerFactory

  /* User */
  @Binds
  @IntoMap
  @WorkerKey(PeriodicSyncWorker::class)
  abstract fun periodicSyncWorker(factory: PeriodicSyncWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncUserActivityWorker::class)
  abstract fun syncUserActivityWorker(factory: SyncUserActivityWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncUserSettingsWorker::class)
  abstract fun syncUserSettingsWorker(factory: SyncUserSettingsWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncWatchedMoviesWorker::class)
  abstract fun syncWatchedMoviesWorker(factory: SyncWatchedMoviesWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncWatchedShowsWorker::class)
  abstract fun syncWatchedShowsWorker(factory: SyncWatchedShowsWorker.Factory): ChildWorkerFactory

  @Binds
  @IntoMap
  @WorkerKey(SyncWatchingWorker::class)
  abstract fun syncWatchingWorker(factory: SyncWatchingWorker.Factory): ChildWorkerFactory
}
