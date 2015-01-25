/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class TimeUtilsTest {

  @Test public void testDateParsing() throws Exception {
    final long dateMillis = 1000000000000L;

    final String[] dates = new String[] {
        "2001-09-08T18:46:40.000-07:00", "2001-09-09T01:46:40.000Z", "2001-09-09T01:46:40Z",
    };

    for (String date : dates) {
      long millis = TimeUtils.getMillis(date);
      assertThat(millis).isEqualTo(dateMillis);
    }
  }

  @Test public void testIsoTime() throws Exception {
    String isoTime = TimeUtils.getIsoTime();
    assertThat(isoTime).contains("Z");
  }
}
