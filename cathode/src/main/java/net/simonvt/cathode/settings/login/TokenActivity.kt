/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.settings.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.work.WorkManager
import dagger.android.AndroidInjection
import net.simonvt.cathode.api.TraktSettings
import net.simonvt.cathode.api.entity.AccessToken
import net.simonvt.cathode.api.service.AuthorizationService
import net.simonvt.cathode.api.service.UsersService
import net.simonvt.cathode.databinding.ActivityLoginTokenBinding
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.settings.Accounts
import net.simonvt.cathode.settings.ProfileSettings
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.Timestamps
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.settings.link.TraktLinkActivity
import net.simonvt.cathode.ui.BaseActivity
import net.simonvt.cathode.ui.HomeActivity
import net.simonvt.cathode.work.PeriodicWorkInitializer
import net.simonvt.cathode.work.WorkManagerUtils
import net.simonvt.cathode.work.user.PeriodicSyncWorker
import javax.inject.Inject

class TokenActivity : BaseActivity(), TokenTask.Callback {

  @Inject
  lateinit var periodicWorkInitializer: PeriodicWorkInitializer
  @Inject
  lateinit var workManager: WorkManager
  @Inject
  lateinit var jobManager: JobManager
  @Inject
  lateinit var traktSettings: TraktSettings

  @Inject
  lateinit var authorizationService: AuthorizationService
  @Inject
  lateinit var usersService: UsersService

  private lateinit var binding: ActivityLoginTokenBinding

  private var task: Int = 0

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidInjection.inject(this)
    task = intent.getIntExtra(LoginActivity.EXTRA_TASK, LoginActivity.TASK_LOGIN)

    binding = ActivityLoginTokenBinding.inflate(layoutInflater)
    setContentView(binding.root)
    binding.retry.setOnClickListener(retryClickListener)

    if (TokenTask.runningInstance != null) {
      TokenTask.runningInstance.setCallback(this)
    } else {
      val code = intent.getStringExtra(EXTRA_CODE)
      TokenTask.start(code, authorizationService, usersService, traktSettings, this)
    }

    setRefreshing(true)
  }

  private val retryClickListener = View.OnClickListener {
    val login = Intent(this, LoginActivity::class.java)
    login.putExtra(LoginActivity.EXTRA_TASK, task)
    startActivity(login)
    finish()
  }

  internal fun setRefreshing(refreshing: Boolean) {
    if (refreshing) {
      binding.buttonContainer.visibility = View.GONE
      binding.progressContainer.visibility = View.VISIBLE
    } else {
      binding.buttonContainer.visibility = View.VISIBLE
      binding.progressContainer.visibility = View.GONE
    }
  }

  override fun onTokenFetched(accessToken: AccessToken) {
    val wasLinked = Settings.get(this).getBoolean(TraktLinkSettings.TRAKT_LINKED, false)

    ProfileSettings.clearProfile(this)
    Timestamps.get(this).edit().remove(Timestamps.LAST_CONFIG_SYNC).apply()

    Accounts.setupAccount(this)

    WorkManagerUtils.enqueueNow(workManager, PeriodicSyncWorker::class.java)
    periodicWorkInitializer.initAuthWork()

    if (task == LoginActivity.TASK_LINK) {
      val traktSync = Intent(this, TraktLinkActivity::class.java)
      startActivity(traktSync)
    } else {
      Settings.get(this)
        .edit()
        .putBoolean(TraktLinkSettings.TRAKT_LINK_PROMPTED, true)
        .putBoolean(TraktLinkSettings.TRAKT_LINKED, true)
        .putBoolean(TraktLinkSettings.TRAKT_AUTH_FAILED, false)
        .apply()

      val home = Intent(this, HomeActivity::class.java)
      startActivity(home)
    }

    finish()
  }

  override fun onTokenFetchedFail(error: Int) {
    setRefreshing(false)
    binding.errorMessage.visibility = View.VISIBLE
    binding.errorMessage.setText(error)
  }

  companion object {

    const val EXTRA_CODE = "code"
  }
}
