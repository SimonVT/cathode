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

package net.simonvt.cathode.settings;

import android.app.Application;
import android.text.format.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class) public class FirstAiredOffsetPreferenceTest {

  static final int HOURS = 3;
  static final long HOURS_MILLIS = HOURS * DateUtils.HOUR_IN_MILLIS;

  @Test public void missingInit() {
    try {
      FirstAiredOffsetPreference.getInstance();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("FirstAiredOffsetPreference not initialized");
    }
  }

  @Test public void setAndGet() throws Exception {
    Application application = RuntimeEnvironment.application;
    FirstAiredOffsetPreference.init(application);
    FirstAiredOffsetPreference instance = FirstAiredOffsetPreference.getInstance();

    instance.set(HOURS);
    assertThat(instance.getOffsetHours()).isEqualTo(HOURS);
    assertThat(instance.getOffsetMillis()).isEqualTo(HOURS_MILLIS);
  }
}
