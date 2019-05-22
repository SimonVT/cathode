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
package net.simonvt.cathode.ui

import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import net.simonvt.cathode.R
import timber.log.Timber

class CalendarEntryActivity : NavigationListenerActivity() {

  override fun onCreate(inState: Bundle?) {
    setTheme(R.style.Theme)
    super.onCreate(inState)
    setContentView(R.layout.activity_details)

    val intent = intent

    if (CalendarContract.ACTION_HANDLE_CUSTOM_EVENT == intent.action) {
      val uriString = intent.getStringExtra(CalendarContract.EXTRA_CUSTOM_APP_URI)
      if (!uriString.isNullOrEmpty()) {
        val uri = Uri.parse(uriString)
        val detailsIntent = when (uri.host) {
          "episode" -> EpisodeDetailsActivity.createIntent(this, uri)
          "season" -> SeasonDetailsActivity.createIntent(this, uri)
          else -> {
            Timber.d("Unknown host ${uri.host}")
            null
          }
        }
        if (detailsIntent != null) {
          startActivity(detailsIntent)
        }
      }
    }

    finish()
  }
}
