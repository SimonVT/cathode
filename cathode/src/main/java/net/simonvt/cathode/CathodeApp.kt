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
package net.simonvt.cathode

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateUtils
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasContentProviderInjector
import dagger.android.HasFragmentInjector
import dagger.android.HasServiceInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.coroutines.runBlocking
import net.simonvt.cathode.actions.PeriodicSync
import net.simonvt.cathode.actions.invokeAsync
import net.simonvt.cathode.actions.user.SyncUserActivity
import net.simonvt.cathode.actions.user.SyncWatching
import net.simonvt.cathode.common.dagger.HasViewInjector
import net.simonvt.cathode.common.event.AuthFailedEvent
import net.simonvt.cathode.common.event.AuthFailedEvent.OnAuthFailedListener
import net.simonvt.cathode.common.event.ItemsUpdatedEvent
import net.simonvt.cathode.common.event.ItemsUpdatedEvent.OnItemsUpdatedListener
import net.simonvt.cathode.common.util.MainHandler
import net.simonvt.cathode.jobqueue.Job
import net.simonvt.cathode.jobqueue.JobListener
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.notification.NotificationHelper
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.settings.login.LoginActivity
import net.simonvt.cathode.sync.jobqueue.JobHandler
import net.simonvt.cathode.ui.HomeActivity
import net.simonvt.cathode.work.PeriodicWorkInitializer
import timber.log.Timber
import javax.inject.Inject

class CathodeApp : Application(), HasActivityInjector, HasFragmentInjector,
  HasSupportFragmentInjector, HasServiceInjector, HasViewInjector, HasContentProviderInjector {

  private var resumedActivityCount: Int = 0
  private var lastSync: Long = 0

  private var appComponent: AppComponent? = null
  private var cathodeComponent: CathodeComponent? = null

  @Inject
  lateinit var jobHandler: JobHandler

  @Inject
  lateinit var periodicWorkInitializer: PeriodicWorkInitializer
  @Inject
  lateinit var jobManager: JobManager
  private lateinit var jobListener: JobListener

  @Inject
  lateinit var periodicSync: PeriodicSync
  @Inject
  lateinit var syncUserActivity: SyncUserActivity
  @Inject
  lateinit var syncWatching: SyncWatching

  @Volatile
  private var injected = false
  @Inject
  lateinit var activityInjector: DispatchingAndroidInjector<Activity>
  @Inject
  lateinit var fragmentInjector: DispatchingAndroidInjector<android.app.Fragment>
  @Inject
  lateinit var supportFragmentInjector: DispatchingAndroidInjector<Fragment>
  @Inject
  lateinit var serviceInjector: DispatchingAndroidInjector<Service>
  @Inject
  lateinit var contentProviderInjector: DispatchingAndroidInjector<ContentProvider>
  @Inject
  lateinit var jobInjector: DispatchingAndroidInjector<Job>
  @Inject
  lateinit var viewInjector: DispatchingAndroidInjector<View>

  private val syncRunnable = object : Runnable {
    override fun run() {
      Timber.d("Performing periodic sync")

      runBlocking {
        periodicSync.invokeAsync(Unit)
      }

      lastSync = System.currentTimeMillis()
      MainHandler.postDelayed(this, SYNC_DELAY)
    }
  }

  private val jobHandlerListener = object : JobHandler.JobHandlerListener {

    override fun onQueueEmpty() {
      Timber.d("Job queue empty")
    }

    override fun onQueueFailed() {
      Timber.d("Job queue failed")
    }
  }

  private val onItemsUpdatedListener = OnItemsUpdatedListener {
    NotificationHelper.schedule(
      this@CathodeApp,
      System.currentTimeMillis() + 5 * DateUtils.MINUTE_IN_MILLIS
    )
  }

  private val authFailedListener = OnAuthFailedListener {
    Timber.i("onAuthFailure")
    Settings.get(this@CathodeApp)
      .edit()
      .putBoolean(TraktLinkSettings.TRAKT_AUTH_FAILED, true)
      .apply()

    val intent = Intent(this@CathodeApp, LoginActivity::class.java)
    intent.action = HomeActivity.ACTION_LOGIN
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val pi = PendingIntent.getActivity(this@CathodeApp, 0, intent, 0)

    createAuthChannel()

    val builder = NotificationCompat.Builder(this@CathodeApp, CHANNEL_ERRORS) //
      .setSmallIcon(R.drawable.ic_noti_error)
      .setTicker(getString(R.string.auth_failed))
      .setContentTitle(getString(R.string.auth_failed))
      .setContentText(getString(R.string.auth_failed_desc))
      .setContentIntent(pi)
      .setPriority(Notification.PRIORITY_HIGH)
      .setAutoCancel(true)

    val nm = NotificationManagerCompat.from(this@CathodeApp)
    nm.notify(AUTH_NOTIFICATION, builder.build())
  }

  override fun onCreate() {
    super.onCreate()
    ensureInjection()

    if (BuildConfig.DEBUG) {
      jobListener = object : JobListener {
        override fun onJobAdded(job: Job) {
          if (!TraktLinkSettings.isLinked(this@CathodeApp)) {
            throw RuntimeException(
              "Added job " + job.key() + " that requires authentication when not authenticated"
            )
          }
        }
      }

      jobManager.addJobListener(jobListener)
    }

    AuthFailedEvent.registerListener(authFailedListener)

    registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks() {

      override fun onActivityResumed(activity: Activity) {
        activityResumed()
      }

      override fun onActivityPaused(activity: Activity) {
        activityPaused()
      }
    })

    ItemsUpdatedEvent.registerListener(onItemsUpdatedListener)

    periodicWorkInitializer.init()
    if (TraktLinkSettings.isLinked(this)) {
      periodicWorkInitializer.initAuthWork()
    } else {
      periodicWorkInitializer.cancelAuthWork()
    }
  }

  fun ensureInjection() {
    if (!injected) {
      synchronized(this) {
        if (!injected) {
          appComponent = DaggerAppComponent.builder().appModule(AppModule(this)).build()
          cathodeComponent = appComponent!!.plusCathodeComponent()
          cathodeComponent!!.inject(this)
          injected = true
        }
      }
    }
  }

  private fun activityResumed() {
    resumedActivityCount++

    if (resumedActivityCount == 1) {
      Timber.d("Starting periodic sync")
      val currentTime = System.currentTimeMillis()
      if (lastSync + SYNC_DELAY < currentTime) {
        syncRunnable.run()
      } else {
        val delay = Math.max(SYNC_DELAY - (currentTime - lastSync), 0)
        MainHandler.postDelayed(syncRunnable, delay)
      }

      jobHandler.registerListener(jobHandlerListener)
    }
  }

  private fun activityPaused() {
    resumedActivityCount--
    if (resumedActivityCount == 0) {
      Timber.d("Pausing periodic sync")
      MainHandler.removeCallbacks(syncRunnable)
      jobHandler.unregisterListener(jobHandlerListener)
    }
  }

  private fun createAuthChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val name = getString(R.string.channel_errors)
      val channel = NotificationChannel(CHANNEL_ERRORS, name, NotificationManager.IMPORTANCE_HIGH)
      channel.enableLights(true)
      channel.enableVibration(true)
      nm.createNotificationChannel(channel)
    }
  }

  override fun activityInjector(): AndroidInjector<Activity>? {
    return activityInjector
  }

  override fun fragmentInjector(): AndroidInjector<android.app.Fragment>? {
    return fragmentInjector
  }

  override fun supportFragmentInjector(): AndroidInjector<Fragment>? {
    return supportFragmentInjector
  }

  override fun serviceInjector(): AndroidInjector<Service>? {
    return serviceInjector
  }

  override fun viewInjector(): AndroidInjector<View>? {
    return viewInjector
  }

  override fun contentProviderInjector(): AndroidInjector<ContentProvider>? {
    ensureInjection()
    return contentProviderInjector
  }

  companion object {

    const val CHANNEL_ERRORS = "channel_errors"

    private const val AUTH_NOTIFICATION = 2

    private const val SYNC_DELAY = 15 * DateUtils.MINUTE_IN_MILLIS
  }
}
