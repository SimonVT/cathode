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
import net.simonvt.cathode.settings.UpcomingTimePreference.UpcomingTimeChangeListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class) public class UpcomingTimePreferenceTest {

  static final int HOURS = 3;
  static final long HOURS_MILLIS = HOURS * 1000L;

  @Before public void setup() {
  }

  @Test public void missingInit() {
    try {
      UpcomingTimePreference.getInstance();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("UpcomingTimePreference not initialized");
    }
  }

  @Test public void setAndGet() throws Exception {
    Application application = RuntimeEnvironment.application;
    UpcomingTimePreference.init(application);
    UpcomingTimePreference instance = UpcomingTimePreference.getInstance();

    instance.set(UpcomingTime.WEEKS_2);
    assertThat(instance.get()).isEqualTo(UpcomingTime.WEEKS_2);
  }

  UpcomingTime postAndGetResult;

  @Test public void postAndGet() throws Exception {
    Application application = RuntimeEnvironment.application;
    UpcomingTimePreference.init(application);
    UpcomingTimePreference instance = UpcomingTimePreference.getInstance();
    UpcomingTimeChangeListener listener = new UpcomingTimeChangeListener() {
      @Override public void onUpcomingTimeChanged(UpcomingTime upcomingTime) {
        postAndGetResult = upcomingTime;
      }
    };
    instance.registerListener(listener);

    instance.set(UpcomingTime.WEEKS_2);
    assertThat(postAndGetResult).isEqualTo(UpcomingTime.WEEKS_2);

    instance.unregisterListener(listener);
  }
}
