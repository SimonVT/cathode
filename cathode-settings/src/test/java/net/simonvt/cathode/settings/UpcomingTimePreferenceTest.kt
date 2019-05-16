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
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.simonvt.cathode.settings.UpcomingTimePreference.UpcomingTimeChangeListener
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpcomingTimePreferenceTest {

  private var postAndGetResult: UpcomingTime? = null

  @Before
  fun setup() {
  }

  @Test
  fun missingInit() {
    try {
      UpcomingTimePreference.getInstance()
    } catch (e: IllegalStateException) {
      assertThat(e.message).isEqualTo("UpcomingTimePreference not initialized")
    }
  }

  @Test
  fun setAndGet() {
    UpcomingTimePreference.init(ApplicationProvider.getApplicationContext<Context>())
    val instance = UpcomingTimePreference.getInstance()

    instance.set(UpcomingTime.WEEKS_2)
    assertThat(instance.get()).isEqualTo(UpcomingTime.WEEKS_2)
  }

  @Test
  fun postAndGet() {
    UpcomingTimePreference.init(ApplicationProvider.getApplicationContext<Context>())
    val instance = UpcomingTimePreference.getInstance()
    val listener = UpcomingTimeChangeListener { upcomingTime -> postAndGetResult = upcomingTime }
    instance.registerListener(listener)

    instance.set(UpcomingTime.WEEKS_2)
    assertThat(postAndGetResult).isEqualTo(UpcomingTime.WEEKS_2)

    instance.unregisterListener(listener)
  }
}
