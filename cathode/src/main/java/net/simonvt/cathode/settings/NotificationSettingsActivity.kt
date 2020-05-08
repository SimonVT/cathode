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
package net.simonvt.cathode.settings

import android.os.Bundle
import net.simonvt.cathode.R
import net.simonvt.cathode.databinding.ActivityToolbarBinding
import net.simonvt.cathode.ui.BaseActivity

class NotificationSettingsActivity : BaseActivity() {

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    val binding = ActivityToolbarBinding.inflate(layoutInflater)
    setContentView(binding.root)
    if (supportFragmentManager.findFragmentByTag(FRAGMENT_SETTINGS) == null) {
      val settings = NotificationSettingsFragment()
      supportFragmentManager.beginTransaction()
        .add(R.id.content, settings, FRAGMENT_SETTINGS)
        .commit()
    }

    binding.toolbarInclude.toolbar.setTitle(R.string.title_settings)
    binding.toolbarInclude.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp)
    binding.toolbarInclude.toolbar.setNavigationOnClickListener { finish() }
  }

  companion object {

    private const val FRAGMENT_SETTINGS =
      "net.simonvt.cathode.settings.SettingsActivity.settingsFragment"
  }
}
