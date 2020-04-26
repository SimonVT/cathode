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

package net.simonvt.cathode.settings.link

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.work.WorkManager
import dagger.android.AndroidInjection
import net.simonvt.cathode.databinding.ActivityTraktLinkBinding
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.settings.TraktTimestamps
import net.simonvt.cathode.ui.BaseActivity
import net.simonvt.cathode.ui.HomeActivity
import net.simonvt.cathode.work.enqueueNow
import net.simonvt.cathode.work.user.PeriodicSyncWorker
import net.simonvt.cathode.work.user.SyncUserSettingsWorker
import javax.inject.Inject

class TraktLinkActivity : BaseActivity() {

  @Inject
  lateinit var workManager: WorkManager

  private lateinit var binding: ActivityTraktLinkBinding

  private lateinit var settings: SharedPreferences

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidInjection.inject(this)
    settings = PreferenceManager.getDefaultSharedPreferences(this)

    binding = ActivityTraktLinkBinding.inflate(layoutInflater)
    setContentView(binding.root)
    binding.sync.setOnClickListener(syncClickListener)
    binding.forget.setOnClickListener(forgetClickListener)
  }

  private val syncClickListener = View.OnClickListener {
    val i = Intent(this, TraktLinkSyncActivity::class.java)
    startActivity(i)
  }

  private val forgetClickListener = View.OnClickListener {
    TraktTimestamps.clear(this)

    settings.edit()
      .putBoolean(TraktLinkSettings.TRAKT_LINKED, true)
      .putBoolean(TraktLinkSettings.TRAKT_LINK_PROMPTED, true)
      .putBoolean(TraktLinkSettings.TRAKT_AUTH_FAILED, false)
      .apply()

    workManager.enqueueNow(SyncUserSettingsWorker::class.java)
    workManager.enqueueNow(PeriodicSyncWorker::class.java)

    val i = Intent(this, HomeActivity::class.java)
    startActivity(i)
    finish()
  }
}
