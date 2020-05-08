/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.work.WorkManager
import dagger.android.AndroidInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.databinding.ActivityTraktLinkSyncBinding
import net.simonvt.cathode.jobqueue.Job
import net.simonvt.cathode.jobqueue.JobManager
import net.simonvt.cathode.ui.BaseActivity
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.HomeActivity
import javax.inject.Inject

class TraktLinkSyncActivity : BaseActivity() {

  @Inject
  lateinit var workManager: WorkManager

  @Inject
  lateinit var jobManager: JobManager

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  private val viewModel: TraktLinkSyncViewModel by viewModels { viewModelFactory }

  private var syncJobs: List<Job>? = null

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidInjection.inject(this)
    setContentView(R.layout.link_sync_progressbar)

    viewModel.localState.observe(this, Observer { jobs ->
      syncJobs = jobs
      updateView()
    })
  }

  private fun updateView() {
    if (syncJobs == null) {
      setContentView(R.layout.link_sync_progressbar)
    } else {
      val binding = ActivityTraktLinkSyncBinding.inflate(layoutInflater)
      setContentView(binding.root)

      binding.sync.setOnClickListener {
        SyncThread(this@TraktLinkSyncActivity, workManager, jobManager, syncJobs).start()

        val home = Intent(this@TraktLinkSyncActivity, HomeActivity::class.java)
        startActivity(home)
        finish()
      }

      binding.forget.setOnClickListener { finish() }
    }
  }
}
