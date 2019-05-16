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

package net.simonvt.cathode.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirstAiredOffsetPreferenceTest {

  @Test
  fun missingInit() {
    try {
      FirstAiredOffsetPreference.getInstance()
    } catch (e: IllegalStateException) {
      assertThat(e.message).isEqualTo("FirstAiredOffsetPreference not initialized")
    }
  }

  @Test
  fun setAndGet() {
    FirstAiredOffsetPreference.init(ApplicationProvider.getApplicationContext<Context>())
    val instance = FirstAiredOffsetPreference.getInstance()

    instance.set(HOURS)
    assertThat(instance.offsetHours).isEqualTo(HOURS)
    assertThat(instance.offsetMillis).isEqualTo(HOURS_MILLIS)
  }

  companion object {

    private const val HOURS = 3
    private const val HOURS_MILLIS = HOURS * DateUtils.HOUR_IN_MILLIS
  }
}
