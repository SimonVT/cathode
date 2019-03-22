package net.simonvt.cathode.work.di

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import net.simonvt.cathode.work.CathodeWorkerFactory
import javax.inject.Singleton

@Module(includes = [WorkerModuleBinds::class, WorkerAssistedModule::class])
class WorkerModule {

  @Provides
  @Singleton
  fun provideWorkManager(context: Context, workerFactory: CathodeWorkerFactory): WorkManager {
    val workConfig = Configuration.Builder().setWorkerFactory(workerFactory)
      .setMinimumLoggingLevel(Log.VERBOSE)
    WorkManager.initialize(context, workConfig.build())
    return WorkManager.getInstance()
  }
}
